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
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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
    val fallLabelForUI      = mutableStateOf("safe")
    val fallProbForUI       = mutableStateOf(0f)
    val continuousFallCount = mutableIntStateOf(0)
    val lastKnownLocation   = mutableStateOf<Location?>(null)

    // Dialogs
    val locationPermissionGranted = mutableStateOf(false)
    val showLocationPermDialog    = mutableStateOf(false)
    val showEmailInputDialog      = mutableStateOf(false)
    val emailSentStatus           = mutableStateOf("")

    val isCalibrating = mutableStateOf(false)
    val batteryLevel     = mutableStateOf(-1)   // -1 = unknown, 0-100 = %
    val batteryUpdatedAt = mutableStateOf(0L)

    // Session analytics
    val postureSessions  = mutableStateListOf<PostureSession>()
    val pitchLogs        = mutableStateListOf<PitchLog>()
    val sessionGoodCount = mutableStateOf(0)
    val sessionBadCount  = mutableStateOf(0)
    val sessionStartTime = mutableStateOf(System.currentTimeMillis())

    // Export state
    val isExporting  = mutableStateOf(false)
    val exportUri    = mutableStateOf<Uri?>(null)
    val exportError  = mutableStateOf<String?>(null)   // shown in UI

    // Data load state — shown in UI when loading fails
    val loadError    = mutableStateOf<String?>(null)
    val firebaseUid  = mutableStateOf<String?>(null)   // for debug

    // =========================================================================
    // CONSTANTS
    // =========================================================================
    private companion object {
        const val TAG = "BleViewModel"

        const val POSTURE_DOWNSAMPLE = 10
        const val POSTURE_WINDOW     = 20
        const val POSTURE_STEP       = 5
        const val BAD_LIMIT          = 3
        const val REMINDER_INTERVAL  = 30f

        const val FALL_WINDOW   = 75
        const val FALL_STEP     = 25
        const val IMPACT_THRESH = 14f
        const val STILL_THRESH  = 15f
        const val STILL_NEEDED  = 5
        const val FALL_COOLDOWN = 2_000L

        const val CONTINUOUS_FALL_LIMIT = 10

        const val AUTO_SAVE_INTERVAL_MS  = 5 * 60 * 1000L
        const val PITCH_LOG_INTERVAL_MS  = 5_000L

        const val GPS_MIN_TIME_MS    = 3_000L
        const val GPS_MIN_DISTANCE_M = 0f
        const val GPS_WAIT_STEP_MS   =   500L
        const val GPS_WAIT_MAX_MS    = 10_000L
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
    // GPS
    // =========================================================================
    private val locationManager by lazy {
        getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val gpsLocationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            // ✅ always prefer GPS
            if (
                lastKnownLocation.value == null ||
                location.accuracy < (lastKnownLocation.value?.accuracy ?: 999f)
            ) {
                lastKnownLocation.value = location
            }

            Log.d(
                TAG,
                "GPS fix lat=${location.latitude} lon=${location.longitude} acc=${location.accuracy}"
            )
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val networkLocationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            // ✅ use network only if no GPS yet
            if (lastKnownLocation.value == null) {

                lastKnownLocation.value = location

                Log.d(
                    TAG,
                    "Network fallback lat=${location.latitude} lon=${location.longitude} acc=${location.accuracy}"
                )
            }
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    private var gpsListenersRegistered = false

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================
    private val sessionRepository = SessionRepository(getApplication())

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
        },
        onBatteryReceived = { pct ->        // ← ADD THIS
            batteryLevel.value     = pct
            batteryUpdatedAt.value = System.currentTimeMillis()
            Log.d(TAG, "Battery: $pct%")
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

        classifier = PostureClassifier(getApplication())
        fallDetector = FallDetector(getApplication())

        val hasFine =
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasFine) {
            startLocationUpdates()
        }
    }

    // =========================================================================
    // GPS — START / STOP
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
            Log.w(TAG, "startLocationUpdates: no location permission"); return
        }

        try {
            val mainLooper = Looper.getMainLooper()

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    GPS_MIN_TIME_MS, GPS_MIN_DISTANCE_M, gpsLocationListener, mainLooper
                )
                val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastGps != null) lastKnownLocation.value = lastGps
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    GPS_MIN_TIME_MS, GPS_MIN_DISTANCE_M, networkLocationListener, mainLooper
                )
                if (lastKnownLocation.value == null) {
                    val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (lastNetwork != null) lastKnownLocation.value = lastNetwork
                }
            }

            gpsListenersRegistered = true
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
        } catch (e: Exception) {
            Log.e(TAG, "stopLocationUpdates: ${e.message}")
        }
    }

    // =========================================================================
    // FIREBASE — LOAD (always runs, no BLE dependency)
    // =========================================================================

    /** Load pitch logs for the given day range — called on init + filter change */
    fun loadPitchDataFromFirebase(days: Int = 30) {
        viewModelScope.launch {
            isLoadingData.value = true
            loadError.value = null
            try {
                val loaded = withContext(Dispatchers.IO) { sessionRepository.loadPitchRange(days) }
                pitchData.clear()
                pitchData.addAll(loaded)
                Log.d(TAG, "loadPitchData: loaded ${loaded.size} docs for days=$days")
                if (loaded.isEmpty()) {
                    loadError.value = "No data found. Make sure the device was connected and recorded data."
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPitchData FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                loadError.value = "Load error: ${e.message}"
            } finally {
                isLoadingData.value = false
            }
        }
    }

    /** Load posture sessions — called on init + Analysis screen open */
    fun loadSessionsFromFirebase() {
        viewModelScope.launch {
            try {
                val uid = withContext(Dispatchers.IO) { sessionRepository.getCurrentUid() }
                firebaseUid.value = uid
                Log.d(TAG, "loadSessions — uid=$uid")

                val sessions = withContext(Dispatchers.IO) { sessionRepository.loadSessions() }
                postureSessions.clear()
                postureSessions.addAll(sessions)
                Log.d(TAG, "Sessions loaded: ${sessions.size}")
            } catch (e: Exception) {
                Log.e(TAG, "loadSessions FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            }
        }
    }

    // =========================================================================
    // EXPORT REPORT — generates HTML with embedded SVG charts, no internet needed
    //
    // Usage:
    //   1. Call exportReport(context) — sets exportUri
    //   2. Observe exportUri in UI and fire share Intent
    //
    // AndroidManifest requirement (add inside <application>):
    //   <provider
    //       android:name="androidx.core.content.FileProvider"
    //       android:authorities="${applicationId}.provider"
    //       android:exported="false"
    //       android:grantUriPermissions="true">
    //       <meta-data
    //           android:name="android.support.FILE_PROVIDER_PATHS"
    //           android:resource="@xml/file_paths" />
    //   </provider>
    //
    // res/xml/file_paths.xml:
    //   <paths>
    //       <cache-path name="shared_files" path="." />
    //   </paths>
    // =========================================================================
    // =========================================================================
    // EXPORT REPORT
    // Uses MediaStore.Downloads on Android 10+ — zero manifest/permission changes
    // Falls back to internal cache + ACTION_SEND on older Android
    // =========================================================================
    fun exportReport(context: Context) {
        viewModelScope.launch {
            isExporting.value = true
            exportError.value = null
            try {
                // Fresh load so all 3 periods are populated
                val freshPitch    = withContext(Dispatchers.IO) { sessionRepository.loadPitchRange(30) }
                val freshSessions = withContext(Dispatchers.IO) { sessionRepository.loadSessions() }
                pitchData.clear();      pitchData.addAll(freshPitch)
                postureSessions.clear(); postureSessions.addAll(freshSessions)

                val html     = withContext(Dispatchers.Default) { buildHtmlReport() }
                val htmlBytes = html.toByteArray(Charsets.UTF_8)
                val fileName = "SmartAlignOra_Report.html"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // ── Android 10+ : MediaStore Downloads — no permission needed ──
                    val resolver = context.contentResolver
                    val cv = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/html")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { it.write(htmlBytes) }
                        cv.clear()
                        cv.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, cv, null, null)

                        exportUri.value = uri
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context,
                                "Report saved to Downloads/SmartAlignOra_Report.html",
                                Toast.LENGTH_LONG).show()
                        }
                        Log.d(TAG, "Report saved to MediaStore Downloads: $uri")
                    } else {
                        throw Exception("MediaStore insert returned null URI")
                    }
                } else {
                    // ── Android 9 and below : share as HTML text via ACTION_SEND ──
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type    = "text/html"
                        putExtra(Intent.EXTRA_SUBJECT, "SmartAlignOra Posture Report")
                        putExtra(Intent.EXTRA_TEXT, html)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share Report").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    Log.d(TAG, "Report shared via ACTION_SEND (Android < 10)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "exportReport FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                exportError.value = "Export failed: ${e.message}"
            } finally {
                isExporting.value = false
            }
        }
    }

    fun clearExportUri() { exportUri.value = null }

    // =========================================================================
    // HTML REPORT BUILDER  — covers Today · 7 Days · 30 Days in one file
    // All charts are pure inline SVG — no internet required, opens in browser
    // =========================================================================
    private fun buildHtmlReport(): String {
        val genTime  = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val nowMs    = System.currentTimeMillis()
        val allPitch = pitchData.toList()
        val sessions = postureSessions.toList()

        // ── Helper: filter pitch by days ──────────────────────────────────────
        fun pitchForDays(days: Int): List<Map<String, Any>> {
            val cutoff = nowMs - days * 24L * 60 * 60 * 1000
            return allPitch.filter { (it["timestamp"] as? Number)?.toLong().let { ts -> ts != null && ts >= cutoff } }
        }

        // ── Helper: filter sessions by days ───────────────────────────────────
        fun sessionsForDays(days: Int): List<PostureSession> {
            val cutoff = nowMs - days * 24L * 60 * 60 * 1000
            val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(cutoff))
            return sessions.filter { it.date >= cutoffDate }
        }

        // ── Build per-period data ─────────────────────────────────────────────
        val p1  = pitchForDays(1);   val s1  = sessionsForDays(1)
        val p7  = pitchForDays(7);   val s7  = sessionsForDays(7)
        val p30 = pitchForDays(30);  val s30 = sessionsForDays(30)

        // ── Global overview (all sessions ever) ───────────────────────────────
        val allGood = sessions.sumOf { it.goodCount }
        val allBad  = sessions.sumOf { it.badCount }
        val allT    = (allGood + allBad).coerceAtLeast(1)
        val allPct  = allGood * 100f / allT
        val allMins = sessions.sumOf { it.durationSeconds } / 60L
        val allAvgP = if (sessions.isNotEmpty()) sessions.map { it.avgPitch }.average() else 0.0
        val grade = when {
            allPct >= 90f -> "A" to "#16a34a"
            allPct >= 75f -> "B" to "#22c55e"
            allPct >= 60f -> "C" to "#f59e0b"
            allPct >= 40f -> "D" to "#ea580c"
            else          -> "F" to "#dc2626"
        }

        // ── Build a period section HTML ───────────────────────────────────────
        fun periodSection(title: String, emoji: String, pitch: List<Map<String, Any>>,
                          sess: List<PostureSession>, gradientId: String,
                          accentColor: String): String {
            val good  = sess.sumOf { it.goodCount }
            val bad   = sess.sumOf { it.badCount }
            val t     = (good + bad).coerceAtLeast(1)
            val pct   = good * 100f / t
            val mins  = sess.sumOf { it.durationSeconds } / 60L
            val avgP  = if (sess.isNotEmpty()) sess.map { it.avgPitch }.average() else 0.0
            val gColor = when {
                pct >= 75f -> "#16a34a"
                pct >= 50f -> "#f59e0b"
                else       -> "#dc2626"
            }

            val pitchSvg = buildPitchSvg(pitch, gradientId)
            val barSvg   = buildBarSvg(sess)
            val tableRows = sess.sortedByDescending { it.date }.joinToString("") { s ->
                val sg = s.goodCount; val sb = s.badCount; val st = (sg + sb).coerceAtLeast(1)
                val sp = sg * 100 / st
                val sc = if (sp >= 70) "#16a34a" else if (sp >= 40) "#f59e0b" else "#dc2626"
                val dur = "${s.durationSeconds / 60}m ${s.durationSeconds % 60}s"
                "<tr><td>${s.date}</td>" +
                        "<td style='color:#16a34a;font-weight:600'>$sg</td>" +
                        "<td style='color:#dc2626;font-weight:600'>$sb</td>" +
                        "<td style='color:$sc;font-weight:700'>$sp%</td>" +
                        "<td>${"%.1f".format(s.avgPitch)}°</td>" +
                        "<td>$dur</td></tr>"
            }

            return """
<div class="period-block" style="border-left:4px solid $accentColor">
  <div class="period-header" style="background:${accentColor}11">
    <span style="font-size:22px">$emoji</span>
    <div>
      <h2 style="color:$accentColor">$title</h2>
      <p>${pitch.size} pitch readings &nbsp;·&nbsp; ${sess.size} sessions &nbsp;·&nbsp; ${mins}m tracked</p>
    </div>
    <div class="period-score" style="background:${gColor}22;color:$gColor;border:2px solid $gColor">
      ${"%.0f".format(pct)}%<br><span style="font-size:10px">good</span>
    </div>
  </div>

  <!-- Mini stat row -->
  <div class="mini-cards">
    <div class="mini-card"><div class="mval" style="color:#16a34a">$good</div><div class="mlbl">Good reads</div></div>
    <div class="mini-card"><div class="mval" style="color:#dc2626">$bad</div><div class="mlbl">Bad reads</div></div>
    <div class="mini-card"><div class="mval" style="color:#7c3aed">${"%.1f".format(avgP)}°</div><div class="mlbl">Avg pitch</div></div>
    <div class="mini-card"><div class="mval" style="color:#0891b2">${mins}m</div><div class="mlbl">Duration</div></div>
  </div>

  <!-- Pitch line chart -->
  <div class="chart-box">
    <div class="chart-title">📈 Pitch Angle Over Time</div>
    <div class="legend">
      <span><span class="dot" style="background:#16a34a"></span>Good (±10°)</span>
      <span><span class="dot" style="background:#dc2626"></span>Forward lean</span>
      <span><span class="dot" style="background:#ea580c"></span>Back lean</span>
    </div>
    $pitchSvg
  </div>

  <!-- Good vs Bad bar chart -->
  <div class="chart-box">
    <div class="chart-title">📊 Good vs Bad Posture per Day</div>
    <div class="legend">
      <span><span class="dot" style="background:#16a34a"></span>Good</span>
      <span><span class="dot" style="background:#dc2626"></span>Bad</span>
    </div>
    $barSvg
  </div>

  ${if (sess.isNotEmpty()) """
  <!-- Sessions table -->
  <div class="chart-box">
    <div class="chart-title">🗓 Sessions in this period</div>
    <table>
      <thead><tr><th>Date</th><th>Good</th><th>Bad</th><th>Score</th><th>Avg Pitch</th><th>Duration</th></tr></thead>
      <tbody>$tableRows</tbody>
    </table>
  </div>""" else ""}
</div>""".trimIndent()
        }

        val section1  = periodSection("Today",    "🌅", p1,  s1,  "grad1", "#2563eb")
        val section7  = periodSection("7 Days",   "📅", p7,  s7,  "grad7", "#7c3aed")
        val section30 = periodSection("30 Days",  "📆", p30, s30, "grad30","#0891b2")

        // ── All-time sessions table ───────────────────────────────────────────
        val allSessionRows = sessions.take(50).joinToString("") { s ->
            val sg = s.goodCount; val sb = s.badCount; val st = (sg + sb).coerceAtLeast(1)
            val sp = sg * 100 / st
            val sc = if (sp >= 70) "#16a34a" else if (sp >= 40) "#f59e0b" else "#dc2626"
            val dur = "${s.durationSeconds / 60}m ${s.durationSeconds % 60}s"
            "<tr><td>${s.date}</td>" +
                    "<td style='color:#16a34a;font-weight:600'>$sg</td>" +
                    "<td style='color:#dc2626;font-weight:600'>$sb</td>" +
                    "<td style='color:$sc;font-weight:700'>$sp%</td>" +
                    "<td>${"%.1f".format(s.avgPitch)}°</td>" +
                    "<td>$dur</td></tr>"
        }

        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>SmartAlignOra Posture Report</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f1f5f9;color:#1e293b}
.header{background:linear-gradient(135deg,#1e3a5f,#2563eb);color:white;padding:32px 24px}
.header h1{font-size:26px;font-weight:800;letter-spacing:-0.5px}
.header p{opacity:.7;font-size:13px;margin-top:6px}
.content{max-width:760px;margin:0 auto;padding:24px 16px}
/* Overview cards */
.ov-cards{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-bottom:24px}
@media(min-width:500px){.ov-cards{grid-template-columns:repeat(4,1fr)}}
.ov-card{background:white;border-radius:14px;padding:16px;box-shadow:0 1px 4px rgba(0,0,0,.07)}
.ov-card .val{font-size:26px;font-weight:800;line-height:1}
.ov-card .lbl{font-size:12px;color:#64748b;margin-top:4px}
/* Grade */
.grade-row{display:flex;align-items:center;gap:16px;background:white;border-radius:14px;padding:20px;margin-bottom:32px;box-shadow:0 1px 4px rgba(0,0,0,.07)}
.grade-circle{width:64px;height:64px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:28px;font-weight:900;flex-shrink:0}
.grade-info h3{font-size:17px;font-weight:700}
.grade-info p{font-size:13px;color:#64748b;margin-top:4px}
.prog-track{background:#e2e8f0;border-radius:99px;height:8px;margin-top:10px;overflow:hidden}
.prog-fill{height:100%;border-radius:99px}
/* Period block */
.period-block{background:white;border-radius:16px;margin-bottom:28px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08)}
.period-header{display:flex;align-items:center;gap:14px;padding:20px}
.period-header h2{font-size:18px;font-weight:800}
.period-header p{font-size:12px;color:#64748b;margin-top:3px}
.period-score{width:60px;height:60px;border-radius:50%;display:flex;flex-direction:column;align-items:center;justify-content:center;font-size:18px;font-weight:900;flex-shrink:0;margin-left:auto}
/* Mini cards */
.mini-cards{display:grid;grid-template-columns:repeat(4,1fr);border-top:1px solid #f1f5f9}
.mini-card{padding:12px;border-right:1px solid #f1f5f9;text-align:center}
.mini-card:last-child{border-right:none}
.mval{font-size:18px;font-weight:800;line-height:1}
.mlbl{font-size:10px;color:#94a3b8;margin-top:3px}
/* Charts */
.chart-box{padding:20px;border-top:1px solid #f1f5f9}
.chart-title{font-size:14px;font-weight:700;margin-bottom:10px;color:#1e293b}
.legend{display:flex;gap:14px;font-size:11px;color:#64748b;margin-bottom:10px;flex-wrap:wrap}
.legend span{display:flex;align-items:center;gap:4px}
.dot{width:8px;height:8px;border-radius:50%;display:inline-block}
/* Table */
table{width:100%;border-collapse:collapse;font-size:12px}
th{background:#f8fafc;color:#475569;font-weight:600;font-size:11px;padding:8px;text-align:left;border-bottom:1px solid #e2e8f0}
td{padding:8px;border-bottom:1px solid #f8fafc}
tr:last-child td{border-bottom:none}
/* Divider */
.section-divider{text-align:center;color:#94a3b8;font-size:11px;padding:8px 0;margin-bottom:24px;border-bottom:1px dashed #e2e8f0}
.footer{text-align:center;color:#94a3b8;font-size:12px;padding:24px 0}
svg text{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}
</style>
</head>
<body>

<div class="header">
  <h1>📐 SmartAlignOra — Posture Report</h1>
  <p>Generated $genTime &nbsp;·&nbsp; ${sessions.size} total sessions &nbsp;·&nbsp; ${allPitch.size} total pitch readings</p>
</div>

<div class="content">

  <!-- ═══ OVERALL OVERVIEW ═══ -->
  <div class="ov-cards">
    <div class="ov-card"><div class="val" style="color:#16a34a">${"%.0f".format(allPct)}%</div><div class="lbl">Overall Good</div></div>
    <div class="ov-card"><div class="val" style="color:#dc2626">${"%.0f".format(100f-allPct)}%</div><div class="lbl">Overall Bad</div></div>
    <div class="ov-card"><div class="val" style="color:#7c3aed">${"%.1f".format(allAvgP)}°</div><div class="lbl">Avg Pitch</div></div>
    <div class="ov-card"><div class="val" style="color:#0891b2">${allMins}m</div><div class="lbl">Total Tracked</div></div>
  </div>

  <div class="grade-row">
    <div class="grade-circle" style="background:${grade.second}22;color:${grade.second};border:2px solid ${grade.second}">${grade.first}</div>
    <div class="grade-info" style="flex:1">
      <h3>Overall Posture Grade</h3>
      <p>${sessions.size} sessions across ${sessions.map{it.date}.distinct().size} days &nbsp;·&nbsp; avg pitch ${"%.1f".format(allAvgP)}°</p>
      <div class="prog-track"><div class="prog-fill" style="width:${"%.0f".format(allPct)}%;background:${grade.second}"></div></div>
    </div>
  </div>

  <div class="section-divider">▼ Breakdown by time period</div>

  <!-- ═══ TODAY ═══ -->
  $section1

  <!-- ═══ LAST 7 DAYS ═══ -->
  $section7

  <!-- ═══ LAST 30 DAYS ═══ -->
  $section30

  <!-- ═══ FULL SESSION HISTORY ═══ -->
  ${if (sessions.isNotEmpty()) """
  <div class="period-block">
    <div class="period-header" style="background:#f8fafc">
      <span style="font-size:22px">📋</span>
      <div><h2 style="color:#1e293b">All Sessions (last ${sessions.size.coerceAtMost(50)})</h2>
           <p>Complete history from Firebase</p></div>
    </div>
    <div class="chart-box">
      <table>
        <thead><tr><th>Date</th><th>Good</th><th>Bad</th><th>Score</th><th>Avg Pitch</th><th>Duration</th></tr></thead>
        <tbody>$allSessionRows</tbody>
      </table>
    </div>
  </div>""" else ""}

</div>
<div class="footer">SmartAlignOra &nbsp;·&nbsp; Stay tall, stay well 🧘</div>
</body>
</html>""".trimIndent()
    }

    // ── Pitch SVG line chart ──────────────────────────────────────────────────
    private fun buildPitchSvg(allPitch: List<Map<String, Any>>, gradId: String): String {
        val w = 700; val h = 200
        val padL = 44; val padR = 12; val padT = 12; val padB = 28
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // Up to 60 readings, evenly sampled if more
        val src = if (allPitch.size <= 60) allPitch
        else allPitch.filterIndexed { i, _ -> i % (allPitch.size / 60 + 1) == 0 }.takeLast(60)
        if (src.isEmpty()) return emptyChartSvg(w, h, "No pitch data for this period")

        val pitches = src.mapNotNull { (it["pitch"] as? Number)?.toFloat() }
        if (pitches.isEmpty()) return emptyChartSvg(w, h, "No pitch data for this period")
        val maxAbs  = pitches.maxOfOrNull { abs(it) }?.coerceAtLeast(15f) ?: 30f
        val midY    = padT + chartH / 2f
        val step    = if (pitches.size > 1) chartW.toFloat() / (pitches.size - 1) else chartW.toFloat()

        val pts = pitches.mapIndexed { i, p ->
            val x = padL + i * step
            val y = midY - (p / (maxAbs + 5f)) * (chartH / 2f)
            Pair(x, y)
        }

        val polyline = pts.joinToString(" ") { (x, y) -> "${"%.1f".format(x)},${"%.1f".format(y)}" }

        val circles = pts.mapIndexed { i, (x, y) ->
            val c = if (pitches[i] > 10f) "#dc2626" else if (pitches[i] < -10f) "#ea580c" else "#16a34a"
            """<circle cx="${"%.1f".format(x)}" cy="${"%.1f".format(y)}" r="3.5" fill="$c" stroke="white" stroke-width="1.2"/>"""
        }.joinToString("\n")

        val gridLines = listOf(-30f, -15f, 0f, 15f, 30f)
            .filter { abs(it) <= maxAbs + 5f }
            .joinToString("\n") { v ->
                val y   = midY - (v / (maxAbs + 5f)) * (chartH / 2f)
                val dash = if (v == 0f) "" else """stroke-dasharray="6,4""""
                val op   = if (v == 0f) "0.3" else "0.1"
                """<line x1="$padL" y1="${"%.1f".format(y)}" x2="${w - padR}" y2="${"%.1f".format(y)}"
                      stroke="#000" stroke-opacity="$op" stroke-width="${if (v == 0f) "1.5" else "1"}" $dash/>
                <text x="${padL - 4}" y="${"%.1f".format(y + 4)}" text-anchor="end" font-size="10" fill="#94a3b8">${v.toInt()}°</text>"""
            }

        // X axis time labels
        val firstTs = (src.firstOrNull()?.get("timestamp") as? Number)?.toLong() ?: 0L
        val lastTs  = (src.lastOrNull()?.get("timestamp")  as? Number)?.toLong() ?: 0L
        val fmt = if (lastTs - firstTs > 24 * 60 * 60 * 1000L)
            SimpleDateFormat("MMM d", Locale.getDefault())
        else
            SimpleDateFormat("HH:mm", Locale.getDefault())
        val firstLabel = if (firstTs > 0) fmt.format(Date(firstTs)) else ""
        val lastLabel  = if (lastTs  > 0) fmt.format(Date(lastTs))  else ""

        return """
<svg viewBox="0 0 $w $h" xmlns="http://www.w3.org/2000/svg" style="width:100%;overflow:visible">
  <defs>
    <linearGradient id="$gradId" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#6b21a8" stop-opacity="0.18"/>
      <stop offset="100%" stop-color="#6b21a8" stop-opacity="0"/>
    </linearGradient>
  </defs>
  $gridLines
  <polyline points="$polyline" fill="none" stroke="#6b21a8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  $circles
  <text x="${padL.toFloat()}" y="${(h - 4).toFloat()}" text-anchor="middle" font-size="10" fill="#94a3b8">$firstLabel</text>
  <text x="${(w - padR).toFloat()}" y="${(h - 4).toFloat()}" text-anchor="middle" font-size="10" fill="#94a3b8">$lastLabel</text>
</svg>""".trimIndent()
    }

    // ── Bar chart: good vs bad per day ────────────────────────────────────────
    private fun buildBarSvg(sessions: List<PostureSession>): String {
        val w = 700; val h = 200
        val padL = 8; val padR = 8; val padT = 8; val padB = 32
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        val grouped = sessions
            .groupBy { it.date }
            .entries.sortedBy { it.key }

        if (grouped.isEmpty()) return emptyChartSvg(w, h, "No sessions in this period")

        val maxVal = grouped.maxOfOrNull { (_, ss) ->
            ss.sumOf { it.goodCount } + ss.sumOf { it.badCount }
        }?.coerceAtLeast(1) ?: 1

        val barGroupW = chartW.toFloat() / grouped.size
        val barW      = (barGroupW * 0.32f).coerceAtLeast(3f)
        val gap       = 1.5f

        val bars = grouped.mapIndexed { i, (date, ss) ->
            val good    = ss.sumOf { it.goodCount }
            val bad     = ss.sumOf { it.badCount }
            val cx      = padL + i * barGroupW + barGroupW / 2f
            val goodH   = (good.toFloat() / maxVal) * chartH
            val badH    = (bad.toFloat()  / maxVal) * chartH
            val baseY   = (padT + chartH).toFloat()
            val label = try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                SimpleDateFormat("MMM d", Locale.getDefault()).format(d ?: Date())
            } catch (_: Exception) { date.takeLast(5) }
            val labelStep = (grouped.size / 7).coerceAtLeast(1)
            val showLabel = i % labelStep == 0 || i == grouped.size - 1

            """${if (goodH > 0) """<rect x="${"%.1f".format(cx - barW - gap)}" y="${"%.1f".format(baseY - goodH)}" width="${"%.1f".format(barW)}" height="${"%.1f".format(goodH)}" rx="3" fill="#16a34a"/>""" else ""}
${if (badH > 0) """<rect x="${"%.1f".format(cx + gap)}" y="${"%.1f".format(baseY - badH)}" width="${"%.1f".format(barW)}" height="${"%.1f".format(badH)}" rx="3" fill="#dc2626"/>""" else ""}
${if (showLabel) """<text x="${"%.1f".format(cx)}" y="${h - 5}" text-anchor="middle" font-size="9" fill="#94a3b8">$label</text>""" else ""}"""
        }.joinToString("\n")

        return """
<svg viewBox="0 0 $w $h" xmlns="http://www.w3.org/2000/svg" style="width:100%">
  <line x1="$padL" y1="${padT + chartH}" x2="${w - padR}" y2="${padT + chartH}" stroke="#e2e8f0" stroke-width="1"/>
  $bars
</svg>""".trimIndent()
    }

    private fun emptyChartSvg(w: Int, h: Int, msg: String) = """
<svg viewBox="0 0 $w $h" xmlns="http://www.w3.org/2000/svg" style="width:100%">
  <rect width="$w" height="$h" rx="8" fill="#f8fafc"/>
  <text x="${w / 2}" y="${h / 2 + 5}" text-anchor="middle" font-size="13" fill="#94a3b8">$msg</text>
</svg>""".trimIndent()

    // =========================================================================
    // SESSION SAVE
    // =========================================================================
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

        if (rawFrameCounter % POSTURE_DOWNSAMPLE == 0 && pitch > -90f && pitch < 90f) {
            currentPitch.value = pitch
            postureBuffer.apply { if (size >= POSTURE_WINDOW) removeFirst(); addLast(pitch) }
            downsampledFrameCounter++
            if (postureBuffer.size >= POSTURE_WINDOW &&
                downsampledFrameCounter % POSTURE_STEP == 0) runPostureML()
        }

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
    // =========================================================================
    private fun runFallDetection(sample: FallDetector.FallSample) {
        if (fallBuffer.size >= FALL_WINDOW) fallBuffer.removeFirst()
        fallBuffer.addLast(sample)
        fallRawCounter++

        stillCounter = if (
            kotlin.math.abs(sample.gx) < STILL_THRESH &&
            kotlin.math.abs(sample.gy) < STILL_THRESH
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
                if (consecutiveFallDetections >= CONTINUOUS_FALL_LIMIT) {
                    fallDetected.value = true
                    if (settingsState.fallAlertEmail.isNotBlank() && !emailSending) {
                        emailSending = true; triggerFallAlertEmail()
                    }
                    consecutiveFallDetections    = 0
                    continuousFallCount.intValue = 0
                }
            }
        }
    }

    // =========================================================================
    // FALL ALERT EMAIL
    // =========================================================================
    private fun triggerFallAlertEmail() {

        val email = settingsState.fallAlertEmail

        if (email.isBlank()) {
            emailSending = false
            return
        }

        emailSentStatus.value = "Getting location..."

        // ✅ make sure GPS running
        startLocationUpdates()

        viewModelScope.launch {

            var location = lastKnownLocation.value

            var waitTime = 0

            // ✅ wait for accurate GPS (max 5 sec)
            while (
                location != null &&
                location.accuracy > 50f &&
                waitTime < 5000
            ) {
                delay(500)
                waitTime += 500
                location = lastKnownLocation.value

                Log.d(
                    TAG,
                    "Waiting for better GPS accuracy = ${location?.accuracy}"
                )
            }

            val time = SimpleDateFormat(
                "dd MMM yyyy, HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            val latitude =
                location?.latitude?.let { "%.6f".format(it) }
                    ?: "Unavailable"

            val longitude =
                location?.longitude?.let { "%.6f".format(it) }
                    ?: "Unavailable"

            val mapsLink =
                if (location != null)
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                else
                    "https://maps.google.com"

            Log.d(
                TAG,
                "EMAIL lat=$latitude lon=$longitude acc=${location?.accuracy}"
            )

            emailSentStatus.value = "Sending emergency email..."

            val success = withContext(Dispatchers.IO) {

                EmailService.sendFallAlert(
                    toEmail = email,
                    time = time,
                    latitude = latitude,
                    longitude = longitude,
                    mapsLink = mapsLink,
                    fallCount = CONTINUOUS_FALL_LIMIT
                )

            }

            emailSentStatus.value =
                if (success)
                    "Emergency email sent"
                else
                    "Email failed"

            emailSending = false

            delay(4000)

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
        if (!settingsState.fallAlertEnabled) { emailSentStatus.value = "Enable Fall Alert first"; return }
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

            if (!locationPermissionGranted.value) {
                requestLocationPermission()
                return
            }

            // ✅ START GPS HERE
            startLocationUpdates()

            showEmailInputDialog.value = true

        } else {

            settingsState = settingsState.copy(
                fallAlertEnabled = false
            )

            fallDetected.value = false

            fallLabelForUI.value = "safe"
            fallProbForUI.value = 0f

            consecutiveFallDetections = 0
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
        if (granted) startLocationUpdates()
        else settingsState = settingsState.copy(fallAlertEnabled = false)
    }

    fun requestLocationPermission() { showLocationPermDialog.value = true }

    override fun onCleared() {
        super.onCleared()
        stopPitchLogTimer()

        if (settingsState.fallAlertEnabled) {
            stopLocationUpdates()
        }

        saveCurrentSession()
        if (isConnected.value) send("0")
        bleService.cleanup()
        fallDetector.close()
        getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
    }
}