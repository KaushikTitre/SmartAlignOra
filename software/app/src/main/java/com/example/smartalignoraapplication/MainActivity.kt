package com.example.smartalignoraapplication

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
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
import java.util.*

class MainActivity : ComponentActivity() {

    /* ---------------- UUIDs ---------------- */
    // These MUST match ESP32 service & characteristic UUIDs
    private val SERVICE_UUID =
        UUID.fromString("a32be81d-570e-4ad9-bf2a-64fdfe3db515")

    private val WRITE_UUID =
        UUID.fromString("c6b0278a-f9b5-4306-8eee-5d74d746bcc3")

    /* ---------------- Bluetooth objects ---------------- */
    // Adapter = phone Bluetooth controller
    private lateinit var adapter: BluetoothAdapter

    // Scanner = used to scan BLE devices
    private lateinit var scanner: BluetoothLeScanner

    // GATT = active BLE connection
    private var gatt: BluetoothGatt? = null

    /* ---------------- UI state ---------------- */
    // Used by Compose to update UI automatically
    private val status = mutableStateOf("Idle")
    private val devices = mutableStateListOf<BluetoothDevice>()

    /* ---------------- Permission launcher ---------------- */
    // Used to request runtime Bluetooth permissions
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.values.all { granted -> granted }) {
                startScan()
            } else {
                status.value = "Permission denied"
            }
        }

    /* ---------------- Bluetooth ON launcher ---------------- */
    // Shows system dialog to turn Bluetooth ON
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startScan()
            } else {
                status.value = "Bluetooth is required"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get Bluetooth system service
        val manager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        adapter = manager.adapter
        scanner = adapter.bluetoothLeScanner

        /* ---------------- UI (Jetpack Compose) ---------------- */
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "SmartAlignOR BLE",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(status.value)

                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { requestPermissionAndBluetooth() }) {
                        Text("Scan BLE Devices")
                    }

                    Spacer(Modifier.height(16.dp))

                    // Show all scanned BLE devices
                    devices.forEach { device ->
                        Text(
                            "${device.name ?: "Unknown"} - ${device.address}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { connect(device) }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }

    /* ---------------- Permission + Bluetooth ON check ---------------- */
    private fun requestPermissionAndBluetooth() {

        // If Bluetooth is OFF, ask user to turn it ON
        if (!adapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(intent)
            return
        }

        // Required permissions depend on Android version
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            else
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) startScan()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    /* ---------------- BLE Scan ---------------- */
    private fun startScan() {
        status.value = "Scanning..."
        devices.clear()
        scanner.startScan(scanCallback)
    }

    // Called every time a BLE device is found
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device
            if (devices.none { it.address == device.address }) {
                devices.add(device)
            }
        }
    }

    /* ---------------- BLE Connect ---------------- */
    private fun connect(device: BluetoothDevice) {
        status.value = "Connecting..."
        gatt?.close()

        // TRANSPORT_LE ensures BLE connection
        gatt =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                device.connectGatt(
                    this,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            else
                device.connectGatt(this, false, gattCallback)
    }

    /* ---------------- GATT callbacks ---------------- */
    private val gattCallback = object : BluetoothGattCallback() {

        // Called when connection state changes
        override fun onConnectionStateChange(
            g: BluetoothGatt,
            statusCode: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                status.value = "Connected"
                g.discoverServices() // Ask ESP32 for services
            }
        }

        // Called after services are discovered
        override fun onServicesDiscovered(g: BluetoothGatt, statusCode: Int) {
            g.getService(SERVICE_UUID)
                ?.getCharacteristic(WRITE_UUID)
                ?.let { sendUnixTimestamp(g, it) }
                ?: run { status.value = "Service not found" }
        }
    }

    /* ---------------- Send Unix Timestamp ---------------- */
    // Sends current time (seconds) to ESP32
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendUnixTimestamp(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val unixTime = (System.currentTimeMillis() / 1000L).toString().toByteArray()

        // Android 13+ non-deprecated API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                unixTime,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            // Older Android support
            characteristic.value = unixTime
            gatt.writeCharacteristic(characteristic)
        }

        status.value = "Timestamp sent\n${String(unixTime)}"
        Log.d("BLE_WRITE", "Sent: ${String(unixTime)}")
    }
}
