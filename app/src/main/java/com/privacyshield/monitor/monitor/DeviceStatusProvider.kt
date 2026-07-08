package com.privacyshield.monitor.monitor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import android.nfc.NfcAdapter
import androidx.core.location.LocationManagerCompat

/**
 * Reads instantaneous system radio/service states for the dashboard tiles.
 * These are global on/off states the platform exposes to any app — Bluetooth,
 * NFC and whether location services are enabled. Purely read-only.
 */
class DeviceStatusProvider(private val context: Context) {

    fun isBluetoothOn(): Boolean {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = mgr?.adapter
        return try {
            adapter?.isEnabled == true
        } catch (e: SecurityException) {
            false
        }
    }

    fun isNfcOn(): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return false
        return adapter.isEnabled
    }

    fun isNfcAvailable(): Boolean = NfcAdapter.getDefaultAdapter(context) != null

    fun isLocationServiceOn(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }
}
