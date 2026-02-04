package com.example.smartalignoraapplication.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.smartalignoraapplication.repository.Esp32Repository
import java.text.SimpleDateFormat
import java.util.*

class BleService(
    private val context: Context,
    private val repository: Esp32Repository,
    private val onStatusChange: (String) -> Unit,
    private val onConnectionChange: (Boolean) -> Unit,
    private val onDataReceived: (String) -> Unit
) {

    private val SERVICE_UUID = UUID.fromString("a32be81d-570e-4ad9-bf2a-64fdfe3db515")
    private val WRITE_UUID   = UUID.fromString("c6b0278a-f9b5-4306-8eee-5d74d746bcc3") // Added back
    private val NOTIFY_UUID  = UUID.fromString("b3af0550-1ba9-4efb-aac7-14287a527e06")
    private val CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isScanning = false
    private var scanAttempt = 0

    fun isBluetoothEnabled() = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!isBluetoothEnabled()) {
            onStatusChange("Bluetooth is disabled")
            return
        }

//        if (scanAttempt >= 5) {
//            onStatusChange("Cooling down...")
//            handler.postDelayed({ scanAttempt = 0; startScanning() }, 3000)
//            return
//        }

        disconnect()

        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            onStatusChange("Scanner not ready")
            return
        }

        onStatusChange("🔍 Searching ESP32...")
        isScanning = true
        scanAttempt++

        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        handler.postDelayed({ if (isScanning) stopScan() }, 10000)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        onConnectionChange(false)
        onStatusChange("Disconnected")
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(type: Int, result: ScanResult) {
            stopScan()
            onStatusChange("Found ESP32. Connecting...")
            connect(result.device)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        gatt?.close()
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            handler.post {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    onConnectionChange(true)
                    onStatusChange("Connected 🟢")

                    g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                    // Delay discovery slightly for stability
                    handler.postDelayed({ g.discoverServices() }, 600)

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onConnectionChange(false)
                    onStatusChange("Disconnected 🔴 Reconnecting...")
                    g.close()
                    gatt = null
                    handler.postDelayed({ startScanning() }, 2000)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(SERVICE_UUID) ?: return

            // 1. Setup Notifications
            val notifyChar = service.getCharacteristic(NOTIFY_UUID)
            if (notifyChar != null) {
                g.setCharacteristicNotification(notifyChar, true)
                val desc = notifyChar.getDescriptor(CCCD_UUID)
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(desc)
            }

            // 2. Send Timestamp immediately (Delayed 200ms to allow Descriptor write to finish)
            val writeChar = service.getCharacteristic(WRITE_UUID)
            if (writeChar != null) {
                handler.postDelayed({
                    val ts = (System.currentTimeMillis() / 1000L).toString()
                    Log.d("BLE", "Sending Timestamp: $ts")

                    // Handle legacy vs new write type
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeCharacteristic(writeChar, ts.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        writeChar.value = ts.toByteArray()
                        writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        g.writeCharacteristic(writeChar)
                    }
                }, 200)
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value.decodeToString().trim()
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            handler.post { onDataReceived("Pitch: $value [$time]") }
            repository.saveReading(value, time)
        }
    }

    fun cleanup() {
        disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}