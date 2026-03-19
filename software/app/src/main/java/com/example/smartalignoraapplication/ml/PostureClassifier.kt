package com.example.smartalignoraapplication.ml

import android.content.Context
import ai.onnxruntime.*
import android.util.Log
import java.nio.FloatBuffer

// =============================================================================
// PostureClassifier — wraps posture_model.onnx
//
// Training label mapping (from training.ipynb):
//   label = 1  if "good" in filename   → GOOD posture
//   label = 0  if "bad"  in filename   → BAD  posture
//
// ONNX output[0] is the predicted class: 0 (BAD) or 1 (GOOD)
//
// classify() returns:
//   1 → GOOD posture
//   0 → BAD  posture
// =============================================================================
class PostureClassifier(context: Context) {

    private companion object {
        const val TAG           = "PostureClassifier"
        const val FEATURE_COUNT = 7L
    }

    private val env     = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    val isLoaded: Boolean get() = session != null

    init {
        try {
            val modelBytes = context.assets.open("posture_model.onnx").readBytes()
            session        = env.createSession(modelBytes, OrtSession.SessionOptions())
            Log.i(TAG, "✅ posture_model.onnx loaded")
            Log.d(TAG, "Inputs:  ${session?.inputNames?.toList()}")
            Log.d(TAG, "Outputs: ${session?.outputNames?.toList()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Model load failed: ${e.message}")
            session = null
        }
    }

    /**
     * Classify a 7-element feature vector.
     * Returns 1 (GOOD posture) or 0 (BAD posture).
     * Falls back to threshold-based rule if model is not loaded.
     */
    fun classify(features: FloatArray): Int {
        val currentSession = session ?: return thresholdFallback(features)

        return try {
            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(features),
                longArrayOf(1L, FEATURE_COUNT)
            )

            val inputName = currentSession.inputNames.iterator().next()
            val results   = currentSession.run(
                mapOf(inputName to inputTensor)
            )

            val prediction = extractPrediction(results[0].value)

            inputTensor.close()
            results.close()

            Log.v(TAG, "Posture prediction=$prediction features=${features.take(3)}")
            prediction

        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            thresholdFallback(features)
        }
    }

    // =========================================================================
    // PREDICTION EXTRACTION
    // ONNX RandomForestClassifier outputs the class label directly.
    // Handles LongArray, Array<LongArray>, Array<FloatArray>, etc.
    // =========================================================================
    private fun extractPrediction(output: Any?): Int {
        return try {
            when (output) {
                // Most common: LongArray  [0] or [1]
                is LongArray -> output[0].toInt()

                is Array<*> -> {
                    val first = output[0]
                    when (first) {
                        is LongArray  -> first[0].toInt()
                        is Long       -> first.toInt()
                        // Probability array: [p_bad, p_good] → argmax
                        is FloatArray -> if (first.size >= 2 && first[1] > first[0]) 1 else 0
                        is DoubleArray-> if (first.size >= 2 && first[1] > first[0]) 1 else 0
                        // String label from some ONNX exporters
                        is String     -> if (first.trim() == "1") 1 else 0
                        else          -> {
                            Log.w(TAG, "Unknown inner type: ${first?.javaClass?.name}")
                            0
                        }
                    }
                }

                is Long   -> output.toInt()
                is Int    -> output

                else -> {
                    Log.w(TAG, "Unknown output type: ${output?.javaClass?.name}")
                    0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractPrediction: ${e.message}")
            0
        }
    }

    // =========================================================================
    // THRESHOLD FALLBACK
    // Used when model is not loaded.
    // features[0] = mean pitch, features[3] = max pitch, features[6] = percent_high
    // =========================================================================
    private fun thresholdFallback(features: FloatArray): Int {
        if (features.isEmpty()) return 0
        val meanPitch    = features.getOrElse(0) { 0f }
        val percentHigh  = features.getOrElse(6) { 0f }
        // GOOD if mean pitch < 10° and less than 20% of samples above HIGH_THRESHOLD
        return if (kotlin.math.abs(meanPitch) < 10f && percentHigh < 0.2f) 1 else 0
    }

    fun close() {
        try { session?.close() } catch (_: Exception) {}
        try { env.close()      } catch (_: Exception) {}
    }
}