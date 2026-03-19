package com.example.smartalignoraapplication.controller

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartalignoraapplication.ml.FeatureExtractor
import com.example.smartalignoraapplication.ml.FallDetector
import com.example.smartalignoraapplication.ml.PostureClassifier
import com.example.smartalignoraapplication.repository.SessionRepository
import com.example.smartalignoraapplication.service.BleService
import com.example.smartalignoraapplication.service.EmailService
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// MOTOR PROTOCOL:
//   S1-S6   → Start pattern (loops until stopped)
//   V1-V100 → Set intensity
//   0       → Stop everything
// =============================================================================

enum class VibrationSequence(val label: String, val bleCommand: String) {
    SHORT       ("Short",        "S1"),
    MEDIUM      ("Medium",       "S2"),
    LONG        ("Long",         "S3"),
    TUK_TUK     ("Tuk Tuk",     "S4"),
    KNOCK_KNOCK ("Knock Knock",  "S5"),
    HEARTBEAT   ("Heartbeat",    "S6")
}

data class SettingsState(
    val vibrationEnabled:  Boolean           = false,
    val vibrationLevel:    Float             = 50f,
    val vibrationSequence: VibrationSequence = VibrationSequence.SHORT,
    val fallAlertEnabled:  Boolean           = false,
    val fallAlertEmail:    String            = ""
)

data class PostureSession(
    val date:            String,
    val goodCount:       Int,
    val badCount:        Int,
    val avgPitch:        Float,
    val durationSeconds: Long
)

data class PitchLog(
    val pitch:        Float,
    val postureLabel: String,
    val timestamp:    Long
)

class BleViewModel(application: Application) : AndroidViewModel(application) {

    // =========================================================================
    // UI STATE
    // =========================================================================
    val isConnected  = mutableStateOf(false)
    val status       = mutableStateOf("Not connected")
    val data         = mutableStateListOf<String>()
    val currentPitch = mutableStateOf(0f)

    val postureState = mutableStateOf("Waiting for posture…")
    val alertState   = mutableStateOf("")

    // ── Analysis data — pitch logs from Firebase ──────────────────────────────
    val pitchData     = mutableStateListOf<Map<String, Any>>()
    val isLoadingData = mutableStateOf(false)

    // ── Fall state ────────────────────────────────────────────────────────────
    val fallDetected        = mutableStateOf(false)
    val fallLabel           = mutableStateOf("safe")
    val fallProbability     = mutableStateOf(0f)
    val continuousFallCount = mutableIntStateOf(0)
    val lastKnownLocation   = mutableStateOf<Location?>(null)

    // ── Dialogs ───────────────────────────────────────────────────────────────
    val locationPermissionGranted = mutableStateOf(false)
    val showLocationPermDialog    = mutableStateOf(false)
    val showEmailInputDialog      = mutableStateOf(false)
    val emailSentStatus           = mutableStateOf("")

    // ── Calibration ───────────────────────────────────────────────────────────
    val isCalibrating = mutableStateOf(false)

    // ── Session analytics ─────────────────────────────────────────────────────
    val postureSessions  = mutableStateListOf<PostureSession>()
    val pitchLogs        = mutableStateListOf<PitchLog>()
    val sessionGoodCount = mutableStateOf(0)
    val sessionBadCount  = mutableStateOf(0)
    val sessionStartTime = mutableStateOf(System.currentTimeMillis())

    // =========================================================================
    // CONSTANTS
    // =========================================================================
    private companion object {
        const val TAG = "BleViewModel"

        const val POSTURE_WINDOW    = 20
        const val POSTURE_STEP      = 5
        const val BAD_LIMIT         = 3
        const val REMINDER_INTERVAL = 30f

        // Fall detection — mirrors Python script values
        const val FALL_WINDOW           = 75
        const val FALL_STEP             = 25
        const val IMPACT_THRESH         = 14f
        const val STILL_THRESH          = 15f
        const val STILL_NEEDED          = 5
        const val FALL_COOLDOWN         = 2_000L
        const val CONTINUOUS_FALL_LIMIT = 10

        const val AUTO_SAVE_INTERVAL_MS = 5 * 60 * 1000L
        const val PITCH_LOG_INTERVAL_MS = 5_000L
        const val GPS_WAIT_MAX_MS       = 10_000L
        const val GPS_CHECK_MS          = 500L
    }

    // =========================================================================
    // PRIVATE VARS
    // =========================================================================
    private val postureBuffer             = ArrayDeque<Float>()
    private var frameCounter              = 0
    private var badCounter                = 0
    private var alertActive               = false
    private var lastAlertTime             = 0f
    private val fallBuffer                = ArrayDeque<FallDetector.FallSample>()
    private var fallCounter               = 0
    private var stillCounter              = 0
    private var lastFallAlert             = 0L
    private var consecutiveFallDetections = 0
    private var lastAutoSaveTime          = System.currentTimeMillis()
    private var motorRunning              = false

    private lateinit var classifier:   PostureClassifier
    private lateinit var fallDetector: FallDetector

    var settingsState by mutableStateOf(SettingsState())
        private set

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================
    private val sessionRepository = SessionRepository()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(getApplication<Application>())
    }
    private var locationCallback: LocationCallback? = null

    // ─── BleService now delivers raw ByteArray instead of String ─────────────
    // onDataReceived receives the raw BLE characteristic bytes.
    // If your BleService still delivers String, see note in parseFullSensorData.
    private val bleService = BleService(
        context            = getApplication(),
        onStatusChange     = { status.value = it },
        onConnectionChange = { connected ->
            isConnected.value = connected
            if (connected) {
                startPitchLogTimer()
                if (settingsState.vibrationEnabled) sendIntensity(settingsState.vibrationLevel)
            } else {
                stopPitchLogTimer()
                saveCurrentSession()
                motorRunning = false
            }
        },
        onDataReceived = { raw ->
            // raw is still String here so we pass it through to the parser.
            // The parser now handles both the new 20-byte binary format
            // (delivered as a raw ByteArray converted to String by BleService)
            // and the legacy CSV fallback.
            data.add(0, raw)
            parseFullSensorData(raw)
        }
    )

    // =========================================================================
    // PITCH LOG TIMER
    // =========================================================================
    private val pitchLogHandler  = Handler(Looper.getMainLooper())
    private var pitchLogRunnable: Runnable? = null

    private fun startPitchLogTimer() {
        stopPitchLogTimer()
        pitchLogRunnable = object : Runnable {
            override fun run() {
                if (isConnected.value) logPitchToFirebase(currentPitch.value, postureState.value)
                pitchLogHandler.postDelayed(this, PITCH_LOG_INTERVAL_MS)
            }
        }
        pitchLogHandler.postDelayed(pitchLogRunnable!!, PITCH_LOG_INTERVAL_MS)
    }

    private fun stopPitchLogTimer() {
        pitchLogRunnable?.let { pitchLogHandler.removeCallbacks(it) }
        pitchLogRunnable = null
    }

    private fun logPitchToFirebase(pitch: Float, postureLabel: String) {
        pitchLogs.add(PitchLog(pitch, postureLabel, System.currentTimeMillis()))
        if (pitchLogs.size > 200) pitchLogs.removeAt(0)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { sessionRepository.savePitchLog(pitch, postureLabel) }
        }
    }

    // =========================================================================
    // BT RECEIVER
    // =========================================================================
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        status.value      = "Bluetooth OFF"
                        isConnected.value = false
                        motorRunning      = false
                        stopPitchLogTimer()
                    }
                    BluetoothAdapter.STATE_ON -> startScan()
                }
            }
        }
    }

    // =========================================================================
    // INIT
    // =========================================================================
    init {
        classifier   = PostureClassifier(getApplication())
        fallDetector = FallDetector(getApplication())
        getApplication<Application>().registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        loadPitchDataFromFirebase()
        Log.i(TAG, "BleViewModel initialized")
    }

    // =========================================================================
    // FIREBASE
    // =========================================================================
    fun loadPitchDataFromFirebase(days: Int = 30) {
        viewModelScope.launch {
            isLoadingData.value = true
            try {
                val loaded = withContext(Dispatchers.IO) { sessionRepository.loadPitchRange(days) }
                pitchData.clear()
                pitchData.addAll(loaded)
                Log.d(TAG, "Pitch loaded: ${loaded.size} records")
            } catch (e: Exception) {
                Log.e(TAG, "loadPitchData: ${e.message}")
            } finally {
                isLoadingData.value = false
            }
        }
    }

    private fun autoSaveSessionSnapshot() {
        val good = sessionGoodCount.value
        val bad  = sessionBadCount.value
        if (good + bad == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionRepository.saveSession(PostureSession(
                    date            = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    goodCount       = good,
                    badCount        = bad,
                    avgPitch        = currentPitch.value,
                    durationSeconds = (System.currentTimeMillis() - sessionStartTime.value) / 1000L
                ))
            }
        }
    }

    private fun saveCurrentSession() {
        val good = sessionGoodCount.value
        val bad  = sessionBadCount.value
        if (good + bad == 0) return
        val session = PostureSession(
            date            = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            goodCount       = good,
            badCount        = bad,
            avgPitch        = currentPitch.value,
            durationSeconds = (System.currentTimeMillis() - sessionStartTime.value) / 1000L
        )
        postureSessions.add(0, session)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { sessionRepository.saveSession(session) }
        }
        sessionGoodCount.value = 0
        sessionBadCount.value  = 0
        sessionStartTime.value = System.currentTimeMillis()
        lastAutoSaveTime       = System.currentTimeMillis()
    }

    // =========================================================================
    // SENSOR PARSER
    //
    // ESP32 binary packet (20 bytes, little-endian):
    //   byte  0-3  : timestamp  uint32  (unused in ML)
    //   byte  4-5  : pitch      int16   ÷ 100
    //   byte  6-7  : roll       int16   ÷ 100
    //   byte  8-9  : Ax         int16   ÷ 100
    //   byte 10-11 : Ay         int16   ÷ 100
    //   byte 12-13 : Az         int16   ÷ 100
    //   byte 14-15 : Gx         int16   ÷ 10
    //   byte 16-17 : Gy         int16   ÷ 10
    //   byte 18-19 : Gz         int16   ÷ 10
    //   acc_mag derived: sqrt(ax²+ay²+az²)
    //
    // If BleService delivers a ByteArray directly, call parseBinaryPacket().
    // If BleService still wraps bytes in a String, use parseFullSensorData()
    // which detects the packet size and routes accordingly.
    // =========================================================================

    /** Call this if your BleService delivers raw ByteArray from the characteristic. */
    fun parseBinaryPacket(bytes: ByteArray) {
        val sample = fallDetector.decodeBlePacket(bytes) ?: return
        onSampleDecoded(sample)
    }

    /** Legacy entry point — called from onDataReceived(String). */
    private fun parseFullSensorData(rawString: String) {
        try {
            // ── Try binary path first ─────────────────────────────────────────
            // BleService may have decoded the characteristic bytes into a
            // Latin-1/ISO-8859-1 string. Re-encode to get the raw bytes back.
            val asBytes = rawString.toByteArray(Charsets.ISO_8859_1)
            if (asBytes.size == FallDetector.PACKET_SIZE) {
                val sample = fallDetector.decodeBlePacket(asBytes)
                if (sample != null) {
                    onSampleDecoded(sample)
                    return
                }
            }

            // ── CSV fallback (legacy / debug mode) ────────────────────────────
            val parts = rawString.trim().split(",")
            if (parts.size >= 10) {
                val ax     = parts[3].trim().toFloatOrNull() ?: 0f
                val ay     = parts[4].trim().toFloatOrNull() ?: 0f
                val az     = parts[5].trim().toFloatOrNull() ?: 0f
                // Use transmitted acc_mag if present, otherwise derive it
                val accMag = parts[6].trim().toFloatOrNull()
                    ?: kotlin.math.sqrt(ax * ax + ay * ay + az * az)
                onSampleDecoded(FallDetector.FallSample(
                    pitch  = parts[1].trim().toFloatOrNull() ?: 0f,
                    roll   = parts[2].trim().toFloatOrNull() ?: 0f,
                    ax     = ax,
                    ay     = ay,
                    az     = az,
                    accMag = accMag,
                    gx     = parts[7].trim().toFloatOrNull() ?: 0f,
                    gy     = parts[8].trim().toFloatOrNull() ?: 0f,
                    gz     = parts[9].trim().toFloatOrNull() ?: 0f
                ))
                return
            }

            // ── Pitch-only fallback ───────────────────────────────────────────
            val pitchOnly = parts[0].trim()
                .replace(Regex("[^0-9.\\-]"), "").toFloatOrNull()
            if (pitchOnly != null && pitchOnly > -90f && pitchOnly < 90f) {
                val tilt = kotlin.math.abs(pitchOnly) > 60f
                onSampleDecoded(FallDetector.FallSample(
                    pitch  = pitchOnly, roll   = 0f,
                    ax     = 0f,        ay     = 0f,
                    az     = if (tilt) 20f else 9.8f,
                    accMag = if (tilt) IMPACT_THRESH + 2f else 9.8f,
                    gx     = if (tilt) 20f else 5f,
                    gy     = 0f,        gz     = 0f
                ))
            }

        } catch (e: Exception) {
            Log.e(TAG, "parseFullSensorData: ${e.message}")
        }
    }

    // =========================================================================
    // SINGLE DECODED SAMPLE HANDLER
    // All three decode paths converge here.
    // =========================================================================
    private fun onSampleDecoded(sample: FallDetector.FallSample) {
        val pitch = sample.pitch

        // ── Posture pipeline ──────────────────────────────────────────────────
        if (pitch > -90f && pitch < 90f) {
            currentPitch.value = pitch
            postureBuffer.apply {
                if (size >= POSTURE_WINDOW) removeFirst()
                addLast(pitch)
            }
            frameCounter++
            if (postureBuffer.size >= POSTURE_WINDOW && frameCounter % POSTURE_STEP == 0) {
                runPostureML()
            }
        }

        // ── Fall pipeline ─────────────────────────────────────────────────────
        runFallDetection(sample)
    }

    // =========================================================================
    // POSTURE ML
    // =========================================================================
    private fun runPostureML() {
        val features   = FeatureExtractor.extract(postureBuffer.toList())
        val prediction = classifier.classify(features)
        handlePosturePrediction(prediction, System.currentTimeMillis() / 1000f)
    }

    private fun handlePosturePrediction(pred: Int, currentTime: Float) {
        val isGood = pred == 1
        postureState.value = if (isGood) "GOOD POSTURE" else "BAD POSTURE"
        if (isGood) sessionGoodCount.value++ else sessionBadCount.value++

        if (!isGood) {
            badCounter++
            when {
                badCounter >= BAD_LIMIT && !alertActive -> {
                    alertState.value = "⚠️ Sustained Bad Posture!"
                    alertActive      = true
                    lastAlertTime    = currentTime
                    startVibrationIfEnabled()
                }
                alertActive && currentTime - lastAlertTime >= REMINDER_INTERVAL -> {
                    alertState.value = "🔔 Still Bad Posture!"
                    lastAlertTime    = currentTime
                    startVibrationIfEnabled()
                }
            }
        } else {
            badCounter       = 0
            alertActive      = false
            alertState.value = ""
            stopVibration()
        }

        val now = System.currentTimeMillis()
        if (now - lastAutoSaveTime >= AUTO_SAVE_INTERVAL_MS) {
            lastAutoSaveTime = now
            autoSaveSessionSnapshot()
        }
    }

    // =========================================================================
    // MOTOR CONTROL
    // =========================================================================
    private fun startVibrationIfEnabled() {
        if (!settingsState.vibrationEnabled || !isConnected.value) return
        send(settingsState.vibrationSequence.bleCommand)
        motorRunning = true
    }

    private fun stopVibration() {
        if (!isConnected.value || !motorRunning) return
        send("0")
        motorRunning = false
    }

    private fun sendIntensity(level: Float) {
        if (!isConnected.value) return
        send("V${level.toInt().coerceIn(1, 100)}")
    }

    // =========================================================================
    // FALL DETECTION
    //
    // Mirrors Python fall_testing.py:
    //   impact      = acc_mag > 14
    //   ml_detected = pred=="fall" AND prob >= 0.85
    //   confirmed   = ml_detected AND (impact OR still >= 5)
    // =========================================================================
    private fun runFallDetection(sample: FallDetector.FallSample) {
        if (fallBuffer.size >= FALL_WINDOW) fallBuffer.removeFirst()
        fallBuffer.addLast(sample)
        fallCounter++

        stillCounter = if (
            kotlin.math.abs(sample.gx) < STILL_THRESH &&
            kotlin.math.abs(sample.gy) < STILL_THRESH &&
            kotlin.math.abs(sample.gz) < STILL_THRESH
        ) stillCounter + 1 else 0

        val mlResult = when {
            fallDetector.isModelLoaded &&
                    fallBuffer.size >= FALL_WINDOW &&
                    fallCounter % FALL_STEP == 0 -> {
                val r = fallDetector.classify(fallBuffer.toList())
                if (r.fallProbability == 0f)
                    FallDetector.FallResult(r.label, if (r.label == "fall") 0.85f else 0.05f)
                else r
            }
            fallBuffer.size < FALL_WINDOW ->
                fallDetector.thresholdFallback(fallBuffer.toList())
            else ->
                FallDetector.FallResult(fallLabel.value, fallProbability.value)
        }

        val impact     = sample.accMag > IMPACT_THRESH
        val mlDetected = mlResult.label == "fall" && mlResult.fallProbability >= 0.85f
        val isStill    = stillCounter >= STILL_NEEDED
        val cooldown   = System.currentTimeMillis() - lastFallAlert > FALL_COOLDOWN
        val confirmed  = cooldown && mlDetected && (impact || isStill)

        fallLabel.value       = mlResult.label
        fallProbability.value = mlResult.fallProbability

        Log.d(TAG, "Fall: label=${mlResult.label} " +
                "prob=${"%.2f".format(mlResult.fallProbability)} " +
                "accMag=${sample.accMag} still=$stillCounter " +
                "impact=$impact mlDetected=$mlDetected confirmed=$confirmed")

        if (confirmed) {
            consecutiveFallDetections++
            continuousFallCount.intValue = consecutiveFallDetections
            lastFallAlert                = System.currentTimeMillis()
            stillCounter                 = 0

            Log.d(TAG, "🚨 FALL #$consecutiveFallDetections confirmed!")

            if (settingsState.fallAlertEnabled) {
                fallDetected.value = true
                if (settingsState.fallAlertEmail.isNotBlank() &&
                    consecutiveFallDetections >= CONTINUOUS_FALL_LIMIT) {
                    triggerFallAlertEmail()
                    consecutiveFallDetections    = 0
                    continuousFallCount.intValue = 0
                }
            }
        } else if (mlResult.label == "safe") {
            consecutiveFallDetections    = 0
            continuousFallCount.intValue = 0
        }
    }

    // =========================================================================
    // GPS
    // =========================================================================
    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) lastKnownLocation.value = loc
                    else requestFreshLocation()
                }
                .addOnFailureListener { requestFreshLocation() }
        } catch (e: Exception) { Log.e(TAG, "fetchLastLocation: ${e.message}") }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation() {
        try {
            val cb = object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    r.lastLocation?.let { lastKnownLocation.value = it }
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                    .setMaxUpdates(1).setWaitForAccurateLocation(false).build(),
                cb, Looper.getMainLooper()
            )
        } catch (e: Exception) { Log.e(TAG, "requestFreshLocation: ${e.message}") }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    r.lastLocation?.let { lastKnownLocation.value = it }
                }
            }
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build(),
                locationCallback!!, Looper.getMainLooper()
            )
        } catch (e: Exception) { Log.e(TAG, "startLocationUpdates: ${e.message}") }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it); locationCallback = null }
    }

    // =========================================================================
    // FALL EMAIL
    // =========================================================================
    private fun triggerFallAlertEmail() {
        val email = settingsState.fallAlertEmail
        if (email.isBlank()) return
        emailSentStatus.value = "📍 Getting GPS location..."
        viewModelScope.launch {
            fetchLastLocation()
            var waited = 0L
            while (lastKnownLocation.value == null && waited < GPS_WAIT_MAX_MS) {
                delay(GPS_CHECK_MS); waited += GPS_CHECK_MS
            }
            val location  = lastKnownLocation.value
            val time      = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
            val latitude  = location?.latitude?.let  { "%.6f".format(it) } ?: "Unavailable"
            val longitude = location?.longitude?.let { "%.6f".format(it) } ?: "Unavailable"
            val mapsLink  = if (location != null)
                "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            else "https://maps.google.com"
            emailSentStatus.value = "📤 Sending emergency email..."
            val success = withContext(Dispatchers.IO) {
                EmailService.sendFallAlert(
                    toEmail   = email, time = time,
                    latitude  = latitude, longitude = longitude,
                    mapsLink  = mapsLink, fallCount = CONTINUOUS_FALL_LIMIT
                )
            }
            emailSentStatus.value = if (success) "✅ Emergency email sent to $email"
            else "❌ Email failed — check credentials in EmailService.kt"
            delay(5000)
            emailSentStatus.value = ""
        }
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================
    fun startScan()          = bleService.startScanning()
    fun disconnect()         = bleService.disconnect()
    fun clearData()          = data.clear()
    fun isBluetoothEnabled() = bleService.isBluetoothEnabled()
    fun send(value: String)  = bleService.sendToEsp32(value)
    fun dismissFallAlert()   { fallDetected.value = false }

    fun calibrate() {
        isCalibrating.value = true
        send("C1")
        Handler(Looper.getMainLooper()).postDelayed({ isCalibrating.value = false }, 3000)
    }

    fun sendEmailManually() {
        if (!settingsState.fallAlertEnabled) { emailSentStatus.value = "⚠️ Enable Fall Alert in Settings first"; return }
        if (settingsState.fallAlertEmail.isBlank()) { emailSentStatus.value = "⚠️ No email configured in Settings"; return }
        if (emailSentStatus.value.startsWith("📤") || emailSentStatus.value.startsWith("📍")) return
        triggerFallAlertEmail()
    }

    fun onVibrationToggled(enabled: Boolean) {
        settingsState = settingsState.copy(vibrationEnabled = enabled)
        if (enabled) sendIntensity(settingsState.vibrationLevel) else { send("0"); motorRunning = false }
    }

    fun onVibrationLevelChanged(level: Float) {
        settingsState = settingsState.copy(vibrationLevel = level)
        if (settingsState.vibrationEnabled && isConnected.value) sendIntensity(level)
    }

    fun onVibrationLevelFinished() {}

    fun onVibrationSequenceChanged(sequence: VibrationSequence) {
        settingsState = settingsState.copy(vibrationSequence = sequence)
        if (settingsState.vibrationEnabled && isConnected.value) { send(sequence.bleCommand); motorRunning = true }
    }

    fun onFallAlertToggled(enabled: Boolean) {
        if (enabled) {
            if (!locationPermissionGranted.value) { requestLocationPermission(); return }
            fetchLastLocation(); startLocationUpdates()
            showEmailInputDialog.value = true
        } else {
            settingsState      = settingsState.copy(fallAlertEnabled = false)
            fallDetected.value = false
            stopLocationUpdates()
        }
    }

    fun onFallAlertEmailConfirmed(email: String) {
        if (email.isNotBlank()) settingsState = settingsState.copy(fallAlertEnabled = true, fallAlertEmail = email)
        showEmailInputDialog.value = false
    }

    fun onFallAlertEmailChanged(email: String) {
        settingsState = settingsState.copy(fallAlertEmail = email)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        locationPermissionGranted.value = granted
        showLocationPermDialog.value    = false
        if (!granted) settingsState = settingsState.copy(fallAlertEnabled = false)
    }

    fun requestLocationPermission() { showLocationPermDialog.value = true }

    override fun onCleared() {
        super.onCleared()
        stopPitchLogTimer(); stopLocationUpdates(); saveCurrentSession()
        if (isConnected.value) send("0")
        bleService.cleanup(); fallDetector.close()
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
    }
}