package com.example.smartalignoraapplication

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.smartalignoraapplication.jetpackcompose.BleScreen
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    // ---------- UI STATE ----------
    private val isConnected = mutableStateOf(false)
    private val status = mutableStateOf("Initializing…")
    private val data = mutableStateListOf<String>()

    // ---------- FIREBASE FIRESTORE ----------
    private val db = FirebaseFirestore.getInstance()

    // ---------- UUIDs ----------
    private val SERVICE_UUID = UUID.fromString("a32be81d-570e-4ad9-bf2a-64fdfe3db515")
    private val WRITE_UUID   = UUID.fromString("c6b0278a-f9b5-4306-8eee-5d74d746bcc3")
    private val NOTIFY_UUID  = UUID.fromString("b3af0550-1ba9-4efb-aac7-14287a527e06")
    private val CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ---------- BLE ----------
    private lateinit var adapter: BluetoothAdapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    // ---------- PERMISSIONS ----------
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) startFilteredScan()
            else status.value = "Bluetooth permission required"
        }

    // ---------- BLUETOOTH ENABLE RESULT ----------
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                status.value = "Bluetooth enabled"
                startBleFlow()
            } else {
                status.value = "Bluetooth required to connect ESP32"
            }
        }

    // ---------- BLUETOOTH STATE LISTENER ----------
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )) {
                    BluetoothAdapter.STATE_OFF -> {
                        status.value = "Bluetooth OFF"
                        isConnected.value = false
                        data.clear()
                    }
                    BluetoothAdapter.STATE_ON -> {
                        status.value = "Bluetooth ON. Reconnecting…"
                        startBleFlow()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        scanner = adapter.bluetoothLeScanner

        registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        setContent {
            BleScreen(
                status = status.value,
                isConnected = isConnected.value,
                receivedData = data,
                onDisconnectClick = { manualDisconnect() },
                onClearDataClick = { data.clear() }
            )
        }

        startBleFlow()
    }

    // ---------- BLE FLOW ----------
    private fun startBleFlow() {
        if (!adapter.isEnabled) {
            status.value = "Please enable Bluetooth"
            enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
            return
        }

        val perms =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            else
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        if (perms.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startFilteredScan()
        } else {
            permissionLauncher.launch(perms)
        }
    }

    // ---------- FILTERED SCAN ----------
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startFilteredScan() {
        status.value = "🔍 Searching ESP32…"
        data.clear()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            scanner?.stopScan(this)
            status.value = "ESP32 found. Connecting…"
            connect(result.device)
        }
    }

    // ---------- CONNECT ----------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connect(device: BluetoothDevice) {
        gatt?.close()
        gatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            g: BluetoothGatt,
            statusCode: Int,
            newState: Int
        ) {
            runOnUiThread {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected.value = true
                    status.value = "Connected to ESP32"
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected.value = false
                    status.value = "Disconnected. Reconnecting…"
                    data.clear()
                    g.close()
                    gatt = null
                    handler.postDelayed({ startFilteredScan() }, 1500)
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, statusCode: Int) {
            val service = g.getService(SERVICE_UUID) ?: return
            val writeChar = service.getCharacteristic(WRITE_UUID) ?: return
            val notifyChar = service.getCharacteristic(NOTIFY_UUID) ?: return

            g.setCharacteristicNotification(notifyChar, true)
            val desc = notifyChar.getDescriptor(CCCD_UUID)
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(desc)

            handler.postDelayed({
                val ts = (System.currentTimeMillis() / 1000L).toString().toByteArray()
                writeChar.value = ts
                g.writeCharacteristic(writeChar)
            }, 200)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value.decodeToString().trim()
            val time =
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            runOnUiThread {
                data.add(0, "Pitch: $value [$time]")
            }

            // Store data in Firebase Firestore
            pushDataToFirestore(value, time)

            Log.d("BLE", "Received Pitch: $value")
        }
    }

    private fun pushDataToFirestore(pitchValue: String, time: String) {
        val entry = hashMapOf(
            "pitch" to pitchValue,
            "timestamp" to time,
            "millis" to System.currentTimeMillis()
        )
        
        db.collection("esp32_readings").add(entry)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }

    private fun manualDisconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        isConnected.value = false
        data.clear()
        status.value = "Disconnected"
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        handler.removeCallbacksAndMessages(null)
        gatt?.close()
    }
}
