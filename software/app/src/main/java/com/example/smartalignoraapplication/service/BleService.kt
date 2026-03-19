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
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// BleService — ESP32 शी BLE connection handle करतो
//
// ✅ Esp32Repository काढला — Firebase save BleViewModel timer करतो
// ✅ onDataReceived callback → BleViewModel ला data मिळतो
// ✅ 5 second Firebase save → BleViewModel मधील pitchLogTimer करतो
// ─────────────────────────────────────────────────────────────────────────────
class BleService(
    private val context:            Context,
    private val onStatusChange:     (String) -> Unit,
    private val onConnectionChange: (Boolean) -> Unit,
    private val onDataReceived:     (String) -> Unit
) {

    private val SERVICE_UUID = UUID.fromString("a32be81d-570e-4ad9-bf2a-64fdfe3db515")
    private val WRITE_UUID   = UUID.fromString("c6b0278a-f9b5-4306-8eee-5d74d746bcc3")
    private val NOTIFY_UUID  = UUID.fromString("b3af0550-1ba9-4efb-aac7-14287a527e06")
    private val CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isScanning  = false
    private var scanAttempt = 0

    fun isBluetoothEnabled() = adapter?.isEnabled == true

    // ─── Scan ─────────────────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!isBluetoothEnabled()) {
            onStatusChange("Bluetooth is disabled")
            return
        }

        disconnect()

        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            onStatusChange("Scanner not ready")
            return
        }

        onStatusChange("🔍 Searching ESP32...")
        isScanning = true
        scanAttempt++

        val filter   = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)

        // Auto stop scan after 10 seconds
        handler.postDelayed({ if (isScanning) stopScan() }, 10_000)
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

    // ─── Scan Callback ────────────────────────────────────────────────────────
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(type: Int, result: ScanResult) {
            stopScan()
            onStatusChange("Found ESP32. Connecting...")
            connect(result.device)
        }
    }

    // ─── Connect ──────────────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        gatt?.close()
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(
                context, false, gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    // ─── GATT Callback ────────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            g: BluetoothGatt, status: Int, newState: Int
        ) {
            handler.post {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        onConnectionChange(true)
                        onStatusChange("Connected 🟢")
                        g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        // Delay for stability
                        handler.postDelayed({ g.discoverServices() }, 600)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        onConnectionChange(false)
                        onStatusChange("Disconnected 🔴 Reconnecting...")
                        g.close()
                        gatt = null
                        // Auto reconnect after 2 seconds
                        handler.postDelayed({ startScanning() }, 2000)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(SERVICE_UUID) ?: run {
                Log.w("BleService", "Service not found")
                return
            }

            // 1. Setup Notifications
            val notifyChar = service.getCharacteristic(NOTIFY_UUID)
            if (notifyChar != null) {
                g.setCharacteristicNotification(notifyChar, true)
                val desc = notifyChar.getDescriptor(CCCD_UUID)
                desc?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(it)
                }
            }

            // 2. Send timestamp to ESP32 (delayed 200ms after descriptor write)
            val writeChar = service.getCharacteristic(WRITE_UUID)
            if (writeChar != null) {
                handler.postDelayed({
                    val ts = (System.currentTimeMillis() / 1000L).toString()
                    Log.d("BleService", "Sending Timestamp: $ts")
                    writeCharacteristic(g, writeChar, ts)
                }, 200)
            }
        }

        // ── Data received from ESP32 ──────────────────────────────────────────
        // ✅ Esp32Repository.saveReading() काढला
        // Firebase save → BleViewModel मधील 5 second timer करतो
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value.decodeToString().trim()
            val time  = SimpleDateFormat(
                "HH:mm:ss", Locale.getDefault()
            ).format(Date())

            // BleViewModel ला raw data पाठवा
            handler.post {
                onDataReceived("$value [$time]")
            }

            // ✅ Firebase save येथे नाही
            // BleViewModel → startPitchLogTimer() → दर 5 seconds → savePitchLog()
        }
    }

    // ─── Write to ESP32 ───────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    fun sendToEsp32(value: String) {
        val g         = gatt ?: return
        val service   = g.getService(SERVICE_UUID) ?: return
        val writeChar = service.getCharacteristic(WRITE_UUID) ?: return
        writeCharacteristic(g, writeChar, value)
        Log.d("BleService", "Sent = $value")
    }

    // ─── Helper — API version compatible write ────────────────────────────────
    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(
        g:    BluetoothGatt,
        char: BluetoothGattCharacteristic,
        data: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(
                char,
                data.toByteArray(),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            @Suppress("DEPRECATION")
            char.value     = data.toByteArray()
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            g.writeCharacteristic(char)
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────
    fun cleanup() {
        disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}