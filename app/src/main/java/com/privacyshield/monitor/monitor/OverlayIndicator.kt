package com.privacyshield.monitor.monitor

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.privacyshield.monitor.core.model.SensorType

/**
 * A small always-on-top dot that appears while the camera or microphone is in
 * use — an iOS-style privacy indicator. It is the strongest possible answer to
 * "an app turned on the mic and hid": the dot lights up no matter which app did
 * it or whether it is visible.
 *
 * Requires the user to grant "display over other apps" (SYSTEM_ALERT_WINDOW);
 * we never assume it and check [canDraw] before touching the window. All window
 * operations are marshalled to the main thread.
 */
class OverlayIndicator(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var dot: View? = null

    fun canDraw(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    /** Show/refresh the dot for the given active sensors, or hide it if none. */
    fun update(activeSensors: Set<SensorType>) {
        main.post {
            if (activeSensors.isEmpty() || !canDraw()) {
                removeInternal()
                return@post
            }
            val color = colorFor(activeSensors)
            val view = dot ?: createDot().also { dot = it; addToWindow(it) }
            (view.background as? GradientDrawable)?.setColor(color)
        }
    }

    fun hide() = main.post { removeInternal() }

    private fun colorFor(sensors: Set<SensorType>): Int = when {
        SensorType.CAMERA in sensors && SensorType.MICROPHONE in sensors -> Color.parseColor("#C62828")
        SensorType.CAMERA in sensors -> Color.parseColor("#2E7D32")
        SensorType.MICROPHONE in sensors -> Color.parseColor("#EF6C00")
        else -> Color.parseColor("#F9A825")
    }

    private fun createDot(): View {
        val sizePx = (14 * context.resources.displayMetrics.density).toInt()
        return View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2E7D32"))
                setStroke((2 * context.resources.displayMetrics.density).toInt(), Color.WHITE)
            }
            minimumWidth = sizePx
            minimumHeight = sizePx
        }
    }

    private fun addToWindow(view: View) {
        val sizePx = (14 * context.resources.displayMetrics.density).toInt()
        val marginPx = (8 * context.resources.displayMetrics.density).toInt()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        val params = WindowManager.LayoutParams(
            sizePx, sizePx, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = marginPx
            y = marginPx
        }
        runCatching { windowManager.addView(view, params) }
    }

    private fun removeInternal() {
        dot?.let { runCatching { windowManager.removeView(it) } }
        dot = null
    }
}
