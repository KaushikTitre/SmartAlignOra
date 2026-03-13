package com.example.smartalignoraapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.smartalignoraapplication.controller.BleViewModel
import com.example.smartalignoraapplication.jetpackcompose.BleScreen

class MainActivity : ComponentActivity() {

    private val controller: BleViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) controller.startScan()
            else controller.status.value = "Permission required"
        }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) checkPermissionsAndStart()
            else controller.status.value = "Bluetooth required"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BleScreen(
                status = controller.status.value,
                isConnected = controller.isConnected.value,
                receivedData = controller.data,

                currentPitch = controller.currentPitch.value,

                // ✅ ADD THESE TWO LINES
                postureState = controller.postureState.value,
                alertState = controller.alertState.value,

                onConnectClick = {
                    if (!controller.isConnected.value)
                        controller.startScan()
                },

                onDisconnectClick = { controller.disconnect() },
                onClearDataClick = { controller.clearData() }
            )

        }

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        if (!controller.isBluetoothEnabled()) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            controller.startScan()
        } else {
            permissionLauncher.launch(perms)
        }
    }
}