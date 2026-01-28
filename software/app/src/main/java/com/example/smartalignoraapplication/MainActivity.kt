package com.example.smartalignoraapplication

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.nio.charset.Charset
import java.util.*

class MainActivity : ComponentActivity() {

    // 🔹 UUIDs (MUST match ESP32)
    private val SERVICE_UUID =
        UUID.fromString("a32be81d-570e-4ad9-bf2a-64fdfe3db515")

    private val WRITE_CHAR_UUID =
        UUID.fromString("c6b0278a-f9b5-4306-8eee-5d74d746bcc3")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val statusText = mutableStateOf("Idle")
    private val devices = mutableStateListOf<BluetoothDevice>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.values.all { granted -> granted }) startScan()
            else statusText.value = "Bluetooth permission denied"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SmartAlignOR BLE", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(statusText.value)
                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { checkPermissions() }) {
                        Text("Scan BLE Devices")
                    }

                    Spacer(Modifier.height(20.dp))

                    devices.forEach { device ->
                        Text(
                            text = "${device.name ?: "Unknown"} - ${device.address}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    statusText.value = "Connecting..."
                                    connect(device)
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val perms =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) startScan()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startScan() {
        statusText.value = "Scanning..."
        devices.clear()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner.startScan(null, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device
            if (!devices.any { it.address == device.address }) {
                devices.add(device)
            }
        }
    }

    private fun connect(device: BluetoothDevice) {
        bluetoothGatt?.close()
        bluetoothGatt =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(this, false, gattCallback)
            }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                statusText.value = "Connected. Discovering services..."
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val ch = gatt.getService(SERVICE_UUID)
                ?.getCharacteristic(WRITE_CHAR_UUID)

            if (ch != null) sendUnixTimestamp(gatt, ch)
            else statusText.value = "Service/Characteristic not found"
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            statusText.value =
                if (status == BluetoothGatt.GATT_SUCCESS)
                    "Timestamp sent successfully"
                else
                    "Failed to send timestamp"
        }
    }

    // 🔹 SAME LOGIC AS YOUR JAVA METHOD
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendUnixTimestamp(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val unixSeconds = System.currentTimeMillis() / 1000L
        val data = unixSeconds.toString().toByteArray(Charset.forName("UTF-8"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gatt.writeCharacteristic(characteristic)
        }

        statusText.value =
            "Connected to ESP32\nUnix Timestamp sent\n$unixSeconds"

        Log.d("BLE_WRITE", "Unix Timestamp: $unixSeconds")
    }
}
