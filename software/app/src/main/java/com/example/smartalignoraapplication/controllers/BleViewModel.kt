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

    // Dependencies
    private val repository = Esp32Repository()
    private val bleService = BleService(
        context = getApplication(),
        repository = repository,
        onStatusChange = { status.value = it },
        onConnectionChange = { isConnected.value = it },
        onDataReceived = { data.add(0, it) }
    )

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