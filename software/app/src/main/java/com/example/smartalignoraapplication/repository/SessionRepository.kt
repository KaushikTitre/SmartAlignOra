package com.example.smartalignoraapplication.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.smartalignoraapplication.controller.PostureSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class SessionRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG        = "SessionRepo"
        private const val PREFS_NAME = "smartalignora_prefs"
        private const val KEY_UID    = "firebase_uid"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Persistent UID ────────────────────────────────────────────────────────
    // Problem: Anonymous auth gives a NEW uid every reinstall/data-clear.
    //          Data saved under old uid becomes invisible forever.
    // Fix:     Save uid to SharedPreferences on first login.
    //          Always use that saved uid for Firestore paths — never changes.
    private suspend fun getUserId(): String {
        val savedUid = prefs.getString(KEY_UID, null)
        if (!savedUid.isNullOrBlank()) {
            Log.d(TAG, "Using saved UID: $savedUid")
            if (auth.currentUser == null) {
                try { auth.signInAnonymously().await() }
                catch (e: Exception) { Log.w(TAG, "Re-sign-in failed: ${e.message}") }
            }
            return savedUid
        }
        // First launch — sign in and save uid permanently
        val currentUser = auth.currentUser
        val uid = if (currentUser != null) {
            currentUser.uid
        } else {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: throw Exception("Auth failed")
        }
        prefs.edit().putString(KEY_UID, uid).apply()
        Log.i(TAG, "First launch — saved UID: $uid")
        return uid
    }

    suspend fun getCurrentUid(): String = getUserId()

    fun overrideSavedUid(uid: String) {
        prefs.edit().putString(KEY_UID, uid).apply()
        Log.i(TAG, "UID overridden to: $uid")
    }

    private suspend fun pitchCollection() =
        db.collection("users").document(getUserId()).collection("pitch")

    private suspend fun sessionsCollection() =
        db.collection("users").document(getUserId()).collection("sessions")

    suspend fun savePitchLog(pitch: Float, postureLabel: String) {
        try {
            val uid = getUserId()
            db.collection("users").document(uid).collection("pitch").add(
                mapOf(
                    "pitch"        to pitch.toDouble(),
                    "postureLabel" to postureLabel,
                    "timestamp"    to System.currentTimeMillis(),
                    "date"         to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "savePitchLog FAILED: ${e.message}", e)
        }
    }

    suspend fun saveSession(session: PostureSession) {
        try {
            val uid = getUserId()
            db.collection("users").document(uid).collection("sessions").add(
                mapOf(
                    "date"            to session.date,
                    "goodCount"       to session.goodCount.toLong(),
                    "badCount"        to session.badCount.toLong(),
                    "avgPitch"        to session.avgPitch.toDouble(),
                    "durationSeconds" to session.durationSeconds,
                    "timestamp"       to System.currentTimeMillis()
                )
            ).await()
            Log.d(TAG, "saveSession OK date=${session.date}")
        } catch (e: Exception) {
            Log.e(TAG, "saveSession FAILED: ${e.message}", e)
        }
    }

    suspend fun loadPitchRange(days: Int): List<Map<String, Any>> {
        val uid      = getUserId()
        val cutoffMs = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        Log.i(TAG, "loadPitchRange days=$days uid=$uid cutoffMs=$cutoffMs")

        return try {
            val docs = db.collection("users").document(uid).collection("pitch")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(3000).get().await().documents

            Log.i(TAG, "Attempt1 raw=${docs.size}")
            val result = docs.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val ts   = (data["timestamp"] as? Number)?.toLong() ?: 0L
                if (ts >= cutoffMs) data else null
            }
            Log.i(TAG, "Attempt1 filtered=${result.size}")

            if (result.isEmpty()) {
                Log.w(TAG, "Attempt1 empty -> no-sort fallback")
                val rawDocs = db.collection("users").document(uid).collection("pitch")
                    .limit(3000).get().await().documents
                Log.i(TAG, "Attempt2 raw=${rawDocs.size}")
                rawDocs.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val ts   = (data["timestamp"] as? Number)?.toLong() ?: 0L
                    if (ts >= cutoffMs) data else null
                }.sortedBy { (it["timestamp"] as? Number)?.toLong() ?: 0L }
                    .also { Log.i(TAG, "Attempt2 filtered=${it.size}") }
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadPitchRange FAILED: ${e.message}", e)
            try {
                val bareDocs = db.collection("users").document(uid).collection("pitch")
                    .limit(500).get().await().documents
                Log.i(TAG, "Bare fallback=${bareDocs.size}")
                bareDocs.mapNotNull { it.data }
            } catch (e2: Exception) {
                Log.e(TAG, "Bare fallback FAILED: ${e2.message}", e2)
                emptyList()
            }
        }
    }

    suspend fun loadSessions(): List<PostureSession> {
        return try {
            val uid = getUserId()
            Log.d(TAG, "loadSessions uid=$uid")
            val docs = db.collection("users").document(uid).collection("sessions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100).get().await().documents
            Log.d(TAG, "loadSessions raw=${docs.size}")
            docs.mapNotNull { doc ->
                try {
                    PostureSession(
                        date            = doc.getString("date")          ?: "",
                        goodCount       = (doc.getLong("goodCount")      ?: 0L).toInt(),
                        badCount        = (doc.getLong("badCount")       ?: 0L).toInt(),
                        avgPitch        = (doc.getDouble("avgPitch")     ?: 0.0).toFloat(),
                        durationSeconds = doc.getLong("durationSeconds") ?: 0L
                    )
                } catch (e: Exception) { null }
            }.also { Log.d(TAG, "loadSessions parsed=${it.size}") }
        } catch (e: Exception) {
            Log.e(TAG, "loadSessions FAILED: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun debugDump() {
        try {
            val uid = getUserId()
            Log.i(TAG, "=== debugDump uid=$uid ===")
            val pitchDocs = db.collection("users").document(uid).collection("pitch")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10).get().await().documents
            Log.i(TAG, "pitch: ${pitchDocs.size} docs")
            pitchDocs.forEach { Log.i(TAG, "  ${it.data}") }
            val sessionDocs = db.collection("users").document(uid).collection("sessions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5).get().await().documents
            Log.i(TAG, "sessions: ${sessionDocs.size} docs")
            sessionDocs.forEach { Log.i(TAG, "  ${it.data}") }
        } catch (e: Exception) {
            Log.e(TAG, "debugDump FAILED: ${e.message}", e)
        }
    }
}