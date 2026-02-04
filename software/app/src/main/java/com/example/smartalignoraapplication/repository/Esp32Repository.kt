package com.example.smartalignoraapplication.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class Esp32Repository {
    private val db = FirebaseFirestore.getInstance()

    fun saveReading(pitchValue: String, time: String) {
        // Run on a background thread to prevent UI freezing ("Davey!" logs)
        Thread {
            val entry = hashMapOf(
                "pitch" to pitchValue,
                "timestamp" to time,
                "millis" to System.currentTimeMillis()
            )

            db.collection("esp32_readings").add(entry)
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error adding document", e)
                }
        }.start()
    }
}