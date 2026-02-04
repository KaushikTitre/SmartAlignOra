//package com.example.smartalignoraapplication.service
//
//import android.util.Log
//import com.google.firebase.firestore.FirebaseFirestore
//
//class FirebaseService {
//
//    // ---------- FIREBASE ----------
//    private val db = FirebaseFirestore.getInstance()
//
//    fun pushDataToFirestore(pitchValue: String, time: String) {
//        val entry = hashMapOf(
//            "pitch" to pitchValue,
//            "timestamp" to time,
//            "millis" to System.currentTimeMillis()
//        )
//
//        db.collection("esp32_readings").add(entry)
//            .addOnSuccessListener { Log.d("Firestore", "Added ID: ${it.id}") }
//            .addOnFailureListener { Log.w("Firestore", "Error adding document", it) }
//    }
//}