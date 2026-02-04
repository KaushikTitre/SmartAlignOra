package com.example.smartalignoraapplication.controller

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.smartalignoraapplication.repository.Esp32Repository
import com.example.smartalignoraapplication.service.BleService

class BleViewModel(application: Application) : AndroidViewModel(application) {

    // UI State
    val isConnected = mutableStateOf(false)
    val status = mutableStateOf("Initializing…")
    val data = mutableStateListOf<String>()

    // NEW: Animation साठी फक्त नंबर (Default 0.0)
    val currentPitch = mutableStateOf(0f)

    // Dependencies
    private val repository = Esp32Repository()
    private val bleService = BleService(
        context = getApplication(),
        repository = repository,
        onStatusChange = { status.value = it },
        onConnectionChange = { isConnected.value = it },
        onDataReceived = { rawString ->
            data.add(0, rawString)

            // --- NEW ROBUST PARSING LOGIC ---
            try {
                // This Regex finds the first decimal number in the string (e.g. "12.5" from "Pitch: 12.5 [10:00]")
                val match = Regex("-?\\d+(\\.\\d+)?").find(rawString)
                if (match != null) {
                    // Check if this number looks like a timestamp (too big) or a pitch angle
                    val number = match.value.toFloat()

                    // Only update pitch if it's a reasonable angle (between -90 and 90)
                    // Timestamps like 1738245... are huge, so we ignore them.
                    if (number > -90 && number < 90) {
                        currentPitch.value = number
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }    )

    // Bluetooth Toggle Receiver
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) {
                    status.value = "Bluetooth OFF"
                    isConnected.value = false
                } else if (state == BluetoothAdapter.STATE_ON) {
                    startScan()
                }
            }
        }
    }

    init {
        getApplication<Application>().registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    fun startScan() = bleService.startScanning()
    fun disconnect() = bleService.disconnect()
    fun clearData() = data.clear()
    fun isBluetoothEnabled() = bleService.isBluetoothEnabled()

    override fun onCleared() {
        super.onCleared()
        bleService.cleanup()
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
    }
}