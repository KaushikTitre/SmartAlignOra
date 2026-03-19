package com.example.smartalignoraapplication.repository

import android.util.Log
import com.example.smartalignoraapplication.controller.PostureSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// SessionRepository — All Firebase operations
//
// Collections:
//   users/{uid}/pitch/    ← दर 5 seconds pitch value
//   users/{uid}/sessions/ ← posture sessions
// ─────────────────────────────────────────────────────────────────────────────
class SessionRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Anonymous auth ────────────────────────────────────────────────────────
    private suspend fun getUserId(): String {
        val user = auth.currentUser
        if (user != null) return user.uid
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw Exception("Auth failed")
    }

    // ── Collection refs ───────────────────────────────────────────────────────
    private suspend fun pitchCollection() =
        db.collection("users").document(getUserId()).collection("pitch")

    private suspend fun sessionsCollection() =
        db.collection("users").document(getUserId()).collection("sessions")

    // ─────────────────────────────────────────────────────────────────────────
    // Save pitch — दर 5 seconds
    // users/{uid}/pitch/{auto-id}
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun savePitchLog(pitch: Float, postureLabel: String) {
        try {
            pitchCollection().add(
                mapOf(
                    "pitch"        to pitch,
                    "postureLabel" to postureLabel,
                    "timestamp"    to System.currentTimeMillis(),
                    "date"         to SimpleDateFormat(
                        "yyyy-MM-dd", Locale.getDefault()
                    ).format(Date())
                )
            ).await()
        } catch (e: Exception) {
            Log.e("DB", "savePitchLog: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save session — disconnect वर / दर 5 min
    // users/{uid}/sessions/{auto-id}
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun saveSession(session: PostureSession) {
        try {
            sessionsCollection().add(
                mapOf(
                    "date"            to session.date,
                    "goodCount"       to session.goodCount,
                    "badCount"        to session.badCount,
                    "avgPitch"        to session.avgPitch,
                    "durationSeconds" to session.durationSeconds,
                    "timestamp"       to System.currentTimeMillis()
                )
            ).await()
            Log.d("DB", "Session saved: ${session.date}")
        } catch (e: Exception) {
            Log.e("DB", "saveSession: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load pitch logs — Analysis screen साठी
    // days=1 → today, days=7 → week, days=30 → month
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun loadPitchRange(days: Int): List<Map<String, Any>> {
        return try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -days)
            val startTime = cal.timeInMillis

            pitchCollection()
                .whereGreaterThan("timestamp", startTime)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(3000)
                .get()
                .await()
                .documents
                .mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("DB", "loadPitchRange: ${e.message}")
            emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load sessions — Analysis / Home screen साठी
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun loadSessions(): List<PostureSession> {
        return try {
            sessionsCollection()
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        PostureSession(
                            date            = doc.getString("date")          ?: "",
                            goodCount       = (doc.getLong("goodCount")      ?: 0L).toInt(),
                            badCount        = (doc.getLong("badCount")       ?: 0L).toInt(),
                            avgPitch        = (doc.getDouble("avgPitch")     ?: 0.0).toFloat(),
                            durationSeconds = doc.getLong("durationSeconds") ?: 0L
                        )
                    } catch (_: Exception) { null }
                }
        } catch (e: Exception) {
            Log.e("DB", "loadSessions: ${e.message}")
            emptyList()
        }
    }
}