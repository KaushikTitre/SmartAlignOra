package com.example.smartalignoraapplication.controller

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartalignoraapplication.ml.FeatureExtractor
import com.example.smartalignoraapplication.ml.FallDetector
import com.example.smartalignoraapplication.ml.PostureClassifier
import com.example.smartalignoraapplication.repository.SessionRepository
import com.example.smartalignoraapplication.service.BleService
import com.example.smartalignoraapplication.service.EmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

// =============================================================================
// MOTOR PROTOCOL:
//   S1-S6   → Start pattern   |   V1-V100 → Intensity   |   0 → Stop
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

    val postureState = mutableStateOf("Waiting for posture...")
    val alertState   = mutableStateOf("")

    val pitchData     = mutableStateListOf<Map<String, Any>>()
    val isLoadingData = mutableStateOf(false)

    // Fall state
    val fallDetected        = mutableStateOf(false)
    val fallLabel           = mutableStateOf("safe")
    val fallProbability     = mutableStateOf(0f)
    val fallLabelForUI      = mutableStateOf("safe")   // "safe" when alertEnabled=false
    val fallProbForUI       = mutableStateOf(0f)
    val continuousFallCount = mutableIntStateOf(0)
    val lastKnownLocation   = mutableStateOf<Location?>(null)

    // Dialogs
    val locationPermissionGranted = mutableStateOf(false)
    val showLocationPermDialog    = mutableStateOf(false)
    val showEmailInputDialog      = mutableStateOf(false)
    val emailSentStatus           = mutableStateOf("")

    val isCalibrating = mutableStateOf(false)

    // Session analytics
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

        // MODEL 1 — Posture (pitch, trained 5 Hz, window=20 = 4 s)
        // ESP32 is 50 Hz → downsample by keeping every 10th frame
        const val POSTURE_DOWNSAMPLE = 10
        const val POSTURE_WINDOW     = 20
        const val POSTURE_STEP       = 5
        const val BAD_LIMIT          = 3
        const val REMINDER_INTERVAL  = 30f

        // MODEL 2 — Fall (all signals, trained 50 Hz, window=75 = 1.5 s)
        const val FALL_WINDOW  = 75
        const val FALL_STEP    = 25
        const val IMPACT_THRESH = 14f
        const val STILL_THRESH  = 15f
        const val STILL_NEEDED  = 5
        const val FALL_COOLDOWN = 2_000L

        // Popup + email only after this many consecutive confirmed falls
        const val CONTINUOUS_FALL_LIMIT = 10

        const val AUTO_SAVE_INTERVAL_MS = 5 * 60 * 1000L
        const val PITCH_LOG_INTERVAL_MS = 5_000L

        // GPS via LocationManager
        const val GPS_MIN_TIME_MS    = 3_000L   // update every 3 s
        const val GPS_MIN_DISTANCE_M = 0f       // update on any movement
        const val GPS_WAIT_STEP_MS   =   500L
        const val GPS_WAIT_MAX_MS    = 10_000L  // max wait for first fix
    }

    // =========================================================================
    // PRIVATE VARS
    // =========================================================================
    private val postureBuffer           = ArrayDeque<Float>()
    private var rawFrameCounter         = 0
    private var downsampledFrameCounter = 0
    private var badCounter              = 0
    private var alertActive             = false
    private var lastAlertTime           = 0f

    private val fallBuffer    = ArrayDeque<FallDetector.FallSample>()
    private var fallRawCounter = 0
    private var stillCounter   = 0
    private var lastFallAlert  = 0L

    private var consecutiveFallDetections = 0
    private var lastAutoSaveTime = System.currentTimeMillis()
    private var motorRunning     = false
    private var emailSending     = false

    private lateinit var classifier:   PostureClassifier
    private lateinit var fallDetector: FallDetector

    var settingsState by mutableStateOf(SettingsState())
        private set

    // =========================================================================
    // GPS — Using Android LocationManager directly
    //
    // WHY NOT FusedLocationProviderClient:
    //   fusedLocationClient.lastLocation returns NULL when no other app has
    //   recently used GPS (very common on fresh boots or when Maps isn't open).
    //   FusedLocationProviderClient also has issues when BLUETOOTH_SCAN is
    //   declared with neverForLocation flag.
    //
    // FIX — Use Android's LocationManager API directly:
    //   • requestLocationUpdates() turns on the GPS hardware directly
    //   • Works independently of Play Services and BLE permission flags
    //   • getLastKnownLocation() returns the last hardware fix (more reliable)
    //   • We request from both GPS_PROVIDER and NETWORK_PROVIDER so we get
    //     a fix even indoors (cell/wifi triangulation as fallback)
    // =========================================================================
    private val locationManager by lazy {
        getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastKnownLocation.value = location
            Log.d(TAG, "GPS fix: lat=${location.latitude} lon=${location.longitude} acc=${location.accuracy}m provider=${location.provider}")
        }
        // Required on older APIs
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String)  {}
        override fun onProviderDisabled(provider: String) {
            Log.w(TAG, "GPS provider disabled: $provider")
        }
    }

    private val networkLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Only use network fix if we don't have a GPS fix yet
            if (lastKnownLocation.value == null) {
                lastKnownLocation.value = location
                Log.d(TAG, "Network fix (fallback): lat=${location.latitude} lon=${location.longitude}")
            }
        }
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String)  {}
        override fun onProviderDisabled(provider: String) {}
    }

    private var gpsListenersRegistered = false

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================
    private val sessionRepository = SessionRepository()

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
        onDataReceived = { bytes ->
            val hex = bytes.take(8).joinToString(" ") { "%02X".format(it) } +
                    if (bytes.size > 8) "..." else ""
            data.add(0, "[${bytes.size}B] $hex")
            if (data.size > 200) data.removeAt(data.lastIndex)
            parseBlePacket(bytes)
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
    // BT STATE RECEIVER
    // =========================================================================
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        status.value = "Bluetooth OFF"; isConnected.value = false
                        motorRunning = false; stopPitchLogTimer()
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
    // GPS — START / STOP
    // Called from MainActivity.onCreate if permission already granted,
    // and from onLocationPermissionResult when permission is newly granted.
    // =========================================================================
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (gpsListenersRegistered) return

        val hasFine = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "startLocationUpdates: no location permission")
            return
        }

        try {
            val mainLooper = Looper.getMainLooper()

            // GPS provider (outdoor — most accurate)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    GPS_MIN_TIME_MS,
                    GPS_MIN_DISTANCE_M,
                    gpsLocationListener,
                    mainLooper
                )
                // Grab last known fix immediately (may be non-null from previous session)
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastGps != null) {
                    lastKnownLocation.value = lastGps
                    Log.d(TAG, "Last GPS fix from cache: ${lastGps.latitude}, ${lastGps.longitude}")
                }
                Log.d(TAG, "GPS_PROVIDER updates started")
            } else {
                Log.w(TAG, "GPS_PROVIDER not enabled — falling back to network")
            }

            // Network provider (indoor fallback — cell/wifi triangulation)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    GPS_MIN_TIME_MS,
                    GPS_MIN_DISTANCE_M,
                    networkLocationListener,
                    mainLooper
                )
                // Use network fix as immediate fallback if no GPS fix yet
                if (lastKnownLocation.value == null) {
                    val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastNetwork != null) {
                        lastKnownLocation.value = lastNetwork
                        Log.d(TAG, "Last network fix from cache: ${lastNetwork.latitude}, ${lastNetwork.longitude}")
                    }
                }
                Log.d(TAG, "NETWORK_PROVIDER updates started")
            }

            gpsListenersRegistered = true
            Log.i(TAG, "Location updates started — current fix: ${lastKnownLocation.value}")

        } catch (e: Exception) {
            Log.e(TAG, "startLocationUpdates: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        if (!gpsListenersRegistered) return
        try {
            locationManager.removeUpdates(gpsLocationListener)
            locationManager.removeUpdates(networkLocationListener)
            gpsListenersRegistered = false
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "stopLocationUpdates: ${e.message}")
        }
    }

    // =========================================================================
    // FIREBASE
    // =========================================================================
    fun loadPitchDataFromFirebase(days: Int = 30) {
        viewModelScope.launch {
            isLoadingData.value = true
            try {
                val loaded = withContext(Dispatchers.IO) { sessionRepository.loadPitchRange(days) }
                pitchData.clear(); pitchData.addAll(loaded)
            } catch (e: Exception) {
                Log.e(TAG, "loadPitchData: ${e.message}")
            } finally {
                isLoadingData.value = false
            }
        }
    }

    private fun autoSaveSessionSnapshot() {
        val good = sessionGoodCount.value; val bad = sessionBadCount.value
        if (good + bad == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionRepository.saveSession(PostureSession(
                    date            = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    goodCount       = good, badCount = bad,
                    avgPitch        = currentPitch.value,
                    durationSeconds = (System.currentTimeMillis() - sessionStartTime.value) / 1000L
                ))
            }
        }
    }

    private fun saveCurrentSession() {
        val good = sessionGoodCount.value; val bad = sessionBadCount.value
        if (good + bad == 0) return
        val session = PostureSession(
            date            = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            goodCount       = good, badCount = bad,
            avgPitch        = currentPitch.value,
            durationSeconds = (System.currentTimeMillis() - sessionStartTime.value) / 1000L
        )
        postureSessions.add(0, session)
        viewModelScope.launch { withContext(Dispatchers.IO) { sessionRepository.saveSession(session) } }
        sessionGoodCount.value = 0; sessionBadCount.value = 0
        sessionStartTime.value = System.currentTimeMillis()
        lastAutoSaveTime       = System.currentTimeMillis()
    }

    // =========================================================================
    // BLE PACKET PARSER
    // =========================================================================
    private fun parseBlePacket(bytes: ByteArray) {
        try {
            if (bytes.size == FallDetector.PACKET_SIZE) {
                val sample = fallDetector.decodeBlePacket(bytes)
                if (sample != null) { onSampleDecoded(sample); return }
            }
            val parts = bytes.decodeToString().trim().split(",")
            if (parts.size >= 9) {
                val ax = parts[3].trim().toFloatOrNull() ?: 0f
                val ay = parts[4].trim().toFloatOrNull() ?: 0f
                val az = parts[5].trim().toFloatOrNull() ?: 0f
                onSampleDecoded(FallDetector.FallSample(
                    pitch  = parts[1].trim().toFloatOrNull() ?: 0f,
                    roll   = parts[2].trim().toFloatOrNull() ?: 0f,
                    ax = ax, ay = ay, az = az,
                    accMag = sqrt(ax * ax + ay * ay + az * az),
                    gx     = parts[6].trim().toFloatOrNull() ?: 0f,
                    gy     = parts[7].trim().toFloatOrNull() ?: 0f,
                    gz     = parts[8].trim().toFloatOrNull() ?: 0f
                ))
            }
        } catch (e: Exception) { Log.e(TAG, "parseBlePacket: ${e.message}") }
    }

    // =========================================================================
    // SAMPLE HANDLER
    // =========================================================================
    private fun onSampleDecoded(sample: FallDetector.FallSample) {
        val pitch = sample.pitch
        rawFrameCounter++

        // Posture: 50 Hz → 5 Hz (keep every 10th frame)
        if (rawFrameCounter % POSTURE_DOWNSAMPLE == 0 && pitch > -90f && pitch < 90f) {
            currentPitch.value = pitch
            postureBuffer.apply { if (size >= POSTURE_WINDOW) removeFirst(); addLast(pitch) }
            downsampledFrameCounter++
            if (postureBuffer.size >= POSTURE_WINDOW &&
                downsampledFrameCounter % POSTURE_STEP == 0) runPostureML()
        }

        // Fall: raw 50 Hz
        runFallDetection(sample)
    }

    // =========================================================================
    // POSTURE ML
    // =========================================================================
    private fun runPostureML() {
        val pred = classifier.classify(FeatureExtractor.extract(postureBuffer.toList()))
        handlePosturePrediction(pred, System.currentTimeMillis() / 1000f)
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
                    alertActive = true; lastAlertTime = currentTime; startVibrationIfEnabled()
                }
                alertActive && currentTime - lastAlertTime >= REMINDER_INTERVAL -> {
                    alertState.value = "🔔 Still Bad Posture!"
                    lastAlertTime = currentTime; startVibrationIfEnabled()
                }
            }
        } else {
            badCounter = 0; alertActive = false; alertState.value = ""; stopVibration()
        }

        val now = System.currentTimeMillis()
        if (now - lastAutoSaveTime >= AUTO_SAVE_INTERVAL_MS) {
            lastAutoSaveTime = now; autoSaveSessionSnapshot()
        }
    }

    // =========================================================================
    // MOTOR CONTROL
    // =========================================================================
    private fun startVibrationIfEnabled() {
        if (!settingsState.vibrationEnabled || !isConnected.value) return
        send(settingsState.vibrationSequence.bleCommand); motorRunning = true
    }
    private fun stopVibration() {
        if (!isConnected.value || !motorRunning) return; send("0"); motorRunning = false
    }
    private fun sendIntensity(level: Float) {
        if (!isConnected.value) return; send("V${level.toInt().coerceIn(1, 100)}")
    }

    // =========================================================================
    // FALL DETECTION
    //
    // Popup + email fire ONLY after CONTINUOUS_FALL_LIMIT (10) consecutive
    // confirmed falls. Counter does NOT reset on "safe" detections.
    // =========================================================================
    private fun runFallDetection(sample: FallDetector.FallSample) {
        if (fallBuffer.size >= FALL_WINDOW) fallBuffer.removeFirst()
        fallBuffer.addLast(sample)
        fallRawCounter++

        stillCounter = if (
            kotlin.math.abs(sample.gx) < STILL_THRESH &&
            kotlin.math.abs(sample.gy) < STILL_THRESH &&
            kotlin.math.abs(sample.gz) < STILL_THRESH
        ) stillCounter + 1 else 0

        val mlResult = when {
            fallDetector.isModelLoaded &&
                    fallBuffer.size >= FALL_WINDOW &&
                    fallRawCounter % FALL_STEP == 0 -> {
                val r = fallDetector.classify(fallBuffer.toList())
                if (r.fallProbability == 0f)
                    FallDetector.FallResult(r.label, if (r.label == "fall") 0.85f else 0.05f)
                else r
            }
            fallBuffer.size < FALL_WINDOW -> fallDetector.thresholdFallback(fallBuffer.toList())
            else -> FallDetector.FallResult(fallLabel.value, fallProbability.value)
        }

        val impact     = sample.accMag > IMPACT_THRESH
        val mlDetected = mlResult.label == "fall" && mlResult.fallProbability >= 0.85f
        val isStill    = stillCounter >= STILL_NEEDED
        val cooldown   = System.currentTimeMillis() - lastFallAlert > FALL_COOLDOWN
        val confirmed  = cooldown && mlDetected && (impact || isStill)

        fallLabel.value       = mlResult.label
        fallProbability.value = mlResult.fallProbability

        // Badge: hidden when alert disabled
        if (settingsState.fallAlertEnabled) {
            fallLabelForUI.value = mlResult.label
            fallProbForUI.value  = mlResult.fallProbability
        } else {
            fallLabelForUI.value = "safe"
            fallProbForUI.value  = 0f
        }

        if (confirmed) {
            lastFallAlert = System.currentTimeMillis()
            stillCounter  = 0

            if (settingsState.fallAlertEnabled) {
                consecutiveFallDetections++
                continuousFallCount.intValue = consecutiveFallDetections
                Log.d(TAG, "Fall confirmed: $consecutiveFallDetections / $CONTINUOUS_FALL_LIMIT")

                if (consecutiveFallDetections >= CONTINUOUS_FALL_LIMIT) {
                    fallDetected.value = true       // triggers popup
                    if (settingsState.fallAlertEmail.isNotBlank() && !emailSending) {
                        emailSending = true
                        triggerFallAlertEmail()
                    }
                    consecutiveFallDetections    = 0
                    continuousFallCount.intValue = 0
                }
            }
        }
    }

    // =========================================================================
    // FALL ALERT EMAIL
    //
    // GPS strategy:
    //   lastKnownLocation is updated every 3 s by LocationManager.
    //   At email time we read it immediately — no waiting needed in most cases.
    //   If it's still null (first boot, GPS cold start) we wait up to 10 s.
    // =========================================================================
    private fun triggerFallAlertEmail() {
        val email = settingsState.fallAlertEmail
        if (email.isBlank()) { emailSending = false; return }

        emailSentStatus.value = "Getting GPS location..."

        viewModelScope.launch {
            // Wait for a fix if we don't have one yet (GPS cold start edge case)
            var waited = 0L
            while (lastKnownLocation.value == null && waited < GPS_WAIT_MAX_MS) {
                delay(GPS_WAIT_STEP_MS); waited += GPS_WAIT_STEP_MS
                Log.d(TAG, "Waiting for GPS fix... ${waited}ms")
            }

            val location  = lastKnownLocation.value
            val time      = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
            val latitude  = location?.latitude?.let  { "%.6f".format(it) } ?: "Unavailable"
            val longitude = location?.longitude?.let { "%.6f".format(it) } ?: "Unavailable"
            val mapsLink  = if (location != null)
                "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            else "https://maps.google.com"

            Log.d(TAG, "Sending email — lat=$latitude lon=$longitude waited=${waited}ms")
            emailSentStatus.value = "Sending emergency email..."

            val success = withContext(Dispatchers.IO) {
                EmailService.sendFallAlert(
                    toEmail   = email,
                    time      = time,
                    latitude  = latitude,
                    longitude = longitude,
                    mapsLink  = mapsLink,
                    fallCount = CONTINUOUS_FALL_LIMIT
                )
            }

            emailSentStatus.value = if (success)
                "Emergency email sent to $email"
            else
                "Email failed — check credentials in EmailService.kt"

            emailSending = false
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
        isCalibrating.value = true; send("C1")
        Handler(Looper.getMainLooper()).postDelayed({ isCalibrating.value = false }, 3000)
    }

    fun sendEmailManually() {
        if (!settingsState.fallAlertEnabled) { emailSentStatus.value = "Enable Fall Alert in Settings first"; return }
        if (settingsState.fallAlertEmail.isBlank()) { emailSentStatus.value = "No email configured"; return }
        if (emailSending) return
        emailSending = true; triggerFallAlertEmail()
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
            showEmailInputDialog.value = true
        } else {
            settingsState                = settingsState.copy(fallAlertEnabled = false)
            fallDetected.value           = false
            fallLabelForUI.value         = "safe"
            fallProbForUI.value          = 0f
            consecutiveFallDetections    = 0
            continuousFallCount.intValue = 0
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
        if (granted) {
            // Start GPS hardware immediately when permission is confirmed
            startLocationUpdates()
        } else {
            settingsState = settingsState.copy(fallAlertEnabled = false)
        }
    }

    fun requestLocationPermission() { showLocationPermDialog.value = true }

    override fun onCleared() {
        super.onCleared()
        stopPitchLogTimer()
        stopLocationUpdates()
        saveCurrentSession()
        if (isConnected.value) send("0")
        bleService.cleanup()
        fallDetector.close()
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
    }
}