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
import com.example.smartalignoraapplication.ml.FeatureExtractor
import com.example.smartalignoraapplication.ml.PostureClassifier

class BleViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ UI STATE

    val isConnected = mutableStateOf(false)
    val status = mutableStateOf("Initializing…")
    val data = mutableStateListOf<String>()

    val currentPitch = mutableStateOf(0f)

    // ✅ ML STATE

    val postureState = mutableStateOf("Waiting for posture…")
    val alertState = mutableStateOf("")

    // ✅ ML CONFIG (Same as Python)

    private companion object {
        const val WINDOW_SIZE = 20
        const val STEP_SIZE = 5
        const val BAD_LIMIT = 3
        const val REMINDER_INTERVAL = 30f
    }

    private val buffer = ArrayDeque<Float>()

    private var frameCounter = 0
    private var badCounter = 0
    private var alertActive = false
    private var lastAlertTime = 0f

    private lateinit var classifier: PostureClassifier

    // ✅ Dependencies

    private val repository = Esp32Repository()

    private val bleService = BleService(
        context = getApplication(),
        repository = repository,
        onStatusChange = { status.value = it },
        onConnectionChange = { isConnected.value = it },
        onDataReceived = { rawString ->

            data.add(0, rawString)

            parsePitch(rawString)
        }
    )

    // ✅ INIT

    init {
        classifier = PostureClassifier(getApplication())
    }

    // ✅ ROBUST PITCH PARSER + ML ENTRY

    private fun parsePitch(rawString: String) {

        try {
            val match = Regex("-?\\d+(\\.\\d+)?").find(rawString)

            if (match != null) {

                val number = match.value.toFloat()

                if (number > -90 && number < 90) {

                    currentPitch.value = number

                    runSlidingWindowML(number)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ✅ SLIDING WINDOW ML LOGIC

    private fun runSlidingWindowML(pitch: Float) {

        val currentTime = System.currentTimeMillis() / 1000f

        if (buffer.size >= WINDOW_SIZE)
            buffer.removeFirst()

        buffer.addLast(pitch)

        if (buffer.size < WINDOW_SIZE) return

        frameCounter++

        if (frameCounter % STEP_SIZE != 0) return

        val features = FeatureExtractor.extract(buffer.toList())

        val prediction = classifier.classify(features)

        handlePrediction(prediction, currentTime)
    }

    // ✅ SMART ALERT LOGIC

    private fun handlePrediction(pred: Int, currentTime: Float) {

        val posture = if (pred == 1) "GOOD POSTURE" else "BAD POSTURE"

        postureState.value = posture

        if (pred == 0) {  // BAD

            badCounter++

            if (badCounter >= BAD_LIMIT && !alertActive) {

                alertState.value = "⚠️ Sustained Bad Posture!"
                alertActive = true
                lastAlertTime = currentTime
            }

            else if (alertActive) {

                if (currentTime - lastAlertTime >= REMINDER_INTERVAL) {

                    alertState.value = "🔔 Still Bad Posture!"
                    lastAlertTime = currentTime
                }
            }

        } else {  // GOOD recovery

            badCounter = 0
            alertActive = false
            alertState.value = ""
        }
    }

    // ✅ Bluetooth Receiver

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                if (state == BluetoothAdapter.STATE_OFF) {
                    status.value = "Bluetooth OFF"
                    isConnected.value = false
                }
                else if (state == BluetoothAdapter.STATE_ON) {
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

    // ✅ PUBLIC FUNCTIONS

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
