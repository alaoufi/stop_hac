package com.privacyshield.monitor.monitor

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Silently captures a front-camera photo of whoever failed to unlock the app,
 * saving it to app-private storage. The image never leaves the device (there is
 * no network code), and the feature is entirely opt-in behind the CAMERA
 * permission — it exists only to identify someone trying to break into your
 * privacy monitor.
 */
object IntruderCapture {

    /** Directory (app-private) where intruder photos are stored. */
    fun directory(context: Context): File =
        File(context.filesDir, "intruders").apply { mkdirs() }

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Binds the front camera headlessly (no preview), takes one photo, then
     * unbinds. Must be called on the main thread with a resumed [lifecycleOwner]
     * (our lock screen satisfies this). Best-effort: failures are swallowed.
     */
    fun capture(context: Context, lifecycleOwner: LifecycleOwner, timestampMillis: Long) {
        if (!hasCameraPermission(context)) return
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture,
                )
            } catch (e: Exception) {
                return@addListener
            }

            val file = File(directory(context), "intruder_$timestampMillis.jpg")
            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        runCatching { provider.unbind(imageCapture) }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        runCatching { provider.unbind(imageCapture) }
                    }
                },
            )
        }, ContextCompat.getMainExecutor(context))
    }

    /** All captured intruder photos, newest first. */
    fun photos(context: Context): List<File> =
        directory(context).listFiles()
            ?.filter { it.isFile && it.extension == "jpg" }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    fun clear(context: Context) {
        directory(context).listFiles()?.forEach { runCatching { it.delete() } }
    }
}
