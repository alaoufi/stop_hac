package com.privacyshield.monitor.monitor

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import com.privacyshield.monitor.R

/**
 * Registering as a device admin is the supported way to make the app
 * meaningfully tamper-resistant: while admin is active, Android blocks the app
 * from being uninstalled until the user first removes admin — which surfaces our
 * warning. We request no invasive policies; being admin alone is the deterrent.
 */
class TamperAdminReceiver : DeviceAdminReceiver() {

    /** Shown when someone tries to disable admin (the step before uninstalling). */
    override fun onDisableRequested(context: Context, intent: android.content.Intent): CharSequence =
        context.getString(R.string.tamper_disable_warning)

    companion object {
        fun component(context: Context): ComponentName =
            ComponentName(context, TamperAdminReceiver::class.java)
    }
}
