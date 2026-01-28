package com.example.smartalignoraapplication

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.smartalignoraapplication.jetpackcompose.BleScreen
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    companion object { private const val TAG = "BLE_MAIN" }
    private val isConnected = mutableStateOf(false)


    // === UUIDs (MUST MATCH ESP32) ===
    private val SERVICE_UUID = UUID.fromString("a32be81d-570e-4ad9-bf2a-64fdfe3db515")
    private val WRITE_UUID   = UUID.fromString("c6b0278a-f9b5-4306-8eee-5d74d746bcc3")
    private val NOTIFY_UUID  = UUID.fromString("b3af0550-1ba9-4efb-aac7-14287a527e06")
    private val CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private lateinit var adapter: BluetoothAdapter
    private lateinit var scanner: BluetoothLeScanner
    private var gatt: BluetoothGatt? = null

    private val status = mutableStateOf("Idle")
    private val devices = mutableStateListOf<BluetoothDevice>()
    private val receivedData = mutableStateListOf<String>()

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    // ===== Launchers =====
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.values.all { granted -> granted }) startScan()
            else status.value = "Permission denied"
        }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) startScan()
            else status.value = "Bluetooth required"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        scanner = adapter.bluetoothLeScanner

        setContent {
            BleScreen(
                status = status.value,
                devices = devices,
                receivedData = receivedData,
                isConnected = isConnected.value,
                onScanClick = { checkBluetooth() },
                onConnectClick = { connect(it) },
                onDisconnectClick = { disconnect() },
                onClearDataClick = { receivedData.clear() }
            )
        }
    }

    // ===== Bluetooth / Permissions =====
    private fun checkBluetooth() {
        if (!adapter.isEnabled) {
            status.value = "Enable Bluetooth"
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }

        val perms =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) startScan()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    // ===== Scan =====
    private fun startScan() {
        if (isScanning) return
        isScanning = true
        devices.clear()
        status.value = "🔍 Scanning..."
        scanner.startScan(scanCallback)

        handler.postDelayed({
            if (isScanning) stopScan()
        }, 10000)
    }

    private fun stopScan() {
        if (!isScanning) return
        isScanning = false
        scanner.stopScan(scanCallback)
        status.value = "Tap ESP32 to connect"
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device
            if (devices.none { it.address == device.address }) {
                devices.add(device)
            }
        }
    }

    // ===== Connect =====
    private fun connect(device: BluetoothDevice) {
        stopScan()
        status.value = "Connecting..."
        gatt?.close()
        gatt =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            else device.connectGatt(this, false, gattCallback)
    }

    private fun disconnect() {
        gatt?.disconnect()
    }

    // ===== GATT CALLBACK =====
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, statusCode: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected")
                isConnected.value = true
                runOnUiThread { status.value = "Connected, discovering..." }
                handler.postDelayed({ g.discoverServices() }, 300)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected")
                runOnUiThread {
                    status.value = "Disconnected, rescanning..."
                    isConnected.value = false
                    receivedData.clear()
                    devices.clear()
                }
                g.close()
                gatt = null
                handler.postDelayed({ startScan() }, 1500)
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, statusCode: Int) {
            if (statusCode != BluetoothGatt.GATT_SUCCESS) return

            val service = g.getService(SERVICE_UUID) ?: return
            val writeChar = service.getCharacteristic(WRITE_UUID)
            val notifyChar = service.getCharacteristic(NOTIFY_UUID)

            // 1️⃣ SEND TIMESTAMP IMMEDIATELY (HANDSHAKE)
            writeChar?.let { sendTimestamp(g, it) }

            // 2️⃣ ENABLE NOTIFY
            notifyChar?.let {
                g.setCharacteristicNotification(it, true)
                handler.postDelayed({
                    val desc = it.getDescriptor(CCCD_UUID)
                    desc?.let { d ->
                        d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        g.writeDescriptor(d)
                    }
                }, 300)
            }

            runOnUiThread { status.value = "Handshake sent, waiting for data..." }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread { this@MainActivity.status.value = "🔔 Notifications enabled" }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value.toString(Charsets.UTF_8)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            runOnUiThread {
                receivedData.add("$value [$time]")
                if (receivedData.size > 50) receivedData.removeAt(0)
                status.value = "LIVE: $value"
            }
        }
    }

    // ===== Timestamp Write =====
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendTimestamp(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic) {
        val ts = (System.currentTimeMillis() / 1000L).toString().toByteArray()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            gatt.writeCharacteristic(ch, ts, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        else {
            ch.value = ts
            gatt.writeCharacteristic(ch)
        }

        Log.d(TAG, "Timestamp sent")
    }
}
