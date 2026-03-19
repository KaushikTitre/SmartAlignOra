package com.example.smartalignoraapplication.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.abs
import kotlin.math.sqrt

class FallDetector(context: Context) {

    data class FallSample(
        val pitch: Float, val roll: Float,
        val ax: Float,    val ay: Float,  val az: Float,
        val accMag: Float,
        val gx: Float,    val gy: Float,  val gz: Float
    )

    data class FallResult(
        val label:           String,  // "fall" or "safe"
        val fallProbability: Float    // 0.0 to 1.0
    )

    companion object {
        private const val TAG           = "FallDetector"
        const val WINDOW_SIZE           = 75
        const val STEP_SIZE             = 25
        private const val FEATURE_COUNT = 69L

        // ─── Binary packet layout (20 bytes, little-endian) ──────────────────
        // byte  0-3  : timestamp  uint32  (raw, unused in ML)
        // byte  4-5  : pitch      int16   ÷ 100  → degrees
        // byte  6-7  : roll       int16   ÷ 100  → degrees
        // byte  8-9  : Ax         int16   ÷ 100  → m/s²
        // byte 10-11 : Ay         int16   ÷ 100  → m/s²
        // byte 12-13 : Az         int16   ÷ 100  → m/s²
        // byte 14-15 : Gx         int16   ÷ 10   → °/s
        // byte 16-17 : Gy         int16   ÷ 10   → °/s
        // byte 18-19 : Gz         int16   ÷ 10   → °/s
        // acc_mag is derived on-device: sqrt(ax²+ay²+az²)
        const val PACKET_SIZE = 20
    }

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    val isModelLoaded: Boolean get() = session != null

    // =========================================================================
    // BINARY PACKET DECODER
    // Call this from BleViewModel instead of parsing CSV strings.
    // Returns null if the byte array is not exactly PACKET_SIZE bytes.
    // =========================================================================
    fun decodeBlePacket(bytes: ByteArray): FallSample? {
        if (bytes.size < PACKET_SIZE) {
            Log.w(TAG, "Packet too short: ${bytes.size} bytes (expected $PACKET_SIZE)")
            return null
        }

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        @Suppress("UNUSED_VARIABLE")
        val timestamp = buf.int.toLong() and 0xFFFFFFFFL   // uint32, kept for logging

        val pitch  = buf.short / 100f
        val roll   = buf.short / 100f
        val ax     = buf.short / 100f
        val ay     = buf.short / 100f
        val az     = buf.short / 100f
        val gx     = buf.short / 10f
        val gy     = buf.short / 10f
        val gz     = buf.short / 10f

        // Derive acc magnitude — same formula used in Python training data collection
        val accMag = sqrt(ax * ax + ay * ay + az * az)

        Log.v(TAG, "BLE decoded: pitch=$pitch roll=$roll " +
                "ax=$ax ay=$ay az=$az accMag=$accMag " +
                "gx=$gx gy=$gy gz=$gz")

        return FallSample(
            pitch  = pitch,
            roll   = roll,
            ax     = ax,
            ay     = ay,
            az     = az,
            accMag = accMag,
            gx     = gx,
            gy     = gy,
            gz     = gz
        )
    }

    // =========================================================================
    // INIT
    // =========================================================================
    init {
        try {
            val bytes = context.assets.open("fall_model.onnx").readBytes()
            session   = env.createSession(bytes, OrtSession.SessionOptions())
            Log.i(TAG, "✅ Model loaded")
            Log.d(TAG, "Outputs: ${session?.outputNames?.toList()}")
            Log.d(TAG, "Inputs:  ${session?.inputNames?.toList()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Model load failed: ${e.message}")
            session = null
        }
    }

    // =========================================================================
    // FEATURE EXTRACTION
    // Mirrors fall_training.py EXACTLY:
    //   signals = ["pitch","roll","ax","ay","az","acc_mag","gx","gy","gz"]
    //   9 signals × 7 stats = 63  +  6 impact features = 69 total
    // =========================================================================
    private fun extractFeatures(window: List<FallSample>): FloatArray {
        val features = mutableListOf<Float>()

        fun addStats(values: List<Float>) {
            val n        = values.size.toFloat()
            val mean     = values.sum() / n
            val variance = values.sumOf { ((it - mean) * (it - mean)).toDouble() } / n
            val std      = sqrt(variance).toFloat()
            val minV     = values.min()
            val maxV     = values.max()
            val range    = maxV - minV
            val rms      = sqrt(values.sumOf { (it * it).toDouble() } / n).toFloat()
            val p2p      = range   // np.ptp == max - min
            features.addAll(listOf(mean, std, minV, maxV, range, rms, p2p))
        }

        // 9 signals × 7 = 63 features (ORDER MUST MATCH TRAINING)
        addStats(window.map { it.pitch  })
        addStats(window.map { it.roll   })
        addStats(window.map { it.ax     })
        addStats(window.map { it.ay     })
        addStats(window.map { it.az     })
        addStats(window.map { it.accMag })
        addStats(window.map { it.gx     })
        addStats(window.map { it.gy     })
        addStats(window.map { it.gz     })

        // 6 impact features
        features.add(window.maxOf { it.accMag })                        // acc_peak
        features.add(window.minOf { it.accMag })                        // acc_min
        features.add(maxOf(                                              // gyro_peak
            window.maxOf { abs(it.gx) },
            window.maxOf { abs(it.gy) },
            window.maxOf { abs(it.gz) }
        ))
        features.add(window.sumOf { it.accMag.toDouble() }.toFloat())   // motion_energy
        features.add(((                                                  // sma_acc
                window.sumOf { abs(it.ax.toDouble()) } +
                        window.sumOf { abs(it.ay.toDouble()) } +
                        window.sumOf { abs(it.az.toDouble()) }
                ) / window.size).toFloat())
        features.add(((                                                  // sma_gyro
                window.sumOf { abs(it.gx.toDouble()) } +
                        window.sumOf { abs(it.gy.toDouble()) } +
                        window.sumOf { abs(it.gz.toDouble()) }
                ) / window.size).toFloat())

        check(features.size == FEATURE_COUNT.toInt()) {
            "Feature count mismatch: got ${features.size}, expected $FEATURE_COUNT"
        }
        return features.toFloatArray()
    }

    // =========================================================================
    // CLASSIFY — ONNX inference
    // =========================================================================
    fun classify(window: List<FallSample>): FallResult {
        val currentSession = session ?: return thresholdFallback(window)

        return try {
            val featureArray = extractFeatures(window)
            val tensor       = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(featureArray),
                longArrayOf(1L, FEATURE_COUNT)
            )

            val inputName = currentSession.inputNames.iterator().next()
            val results   = currentSession.run(
                Collections.singletonMap(inputName, tensor)
            )

            // Output[0] — predicted label
            val output0  = results[0].value
            val rawLabel = when (output0) {
                is Array<*>  -> output0.firstOrNull()?.toString() ?: "safe"
                is List<*>   -> output0.firstOrNull()?.toString() ?: "safe"
                is String    -> output0
                is ByteArray -> String(output0).trim()
                else         -> output0?.toString() ?: "safe"
            }
            val label = rawLabel
                .lowercase().trim()
                .replace("[", "").replace("]", "").replace("'", "")
                .trim()

            // Output[1] — probabilities
            var fallProb = 0f
            try {
                fallProb = parseProb(results[1].value, label)
            } catch (e: Exception) {
                Log.w(TAG, "Prob parse failed: ${e.message}")
                fallProb = if (label.contains("fall")) 0.85f else 0.10f
            }

            tensor.close()
            results.close()

            if (fallProb == 0f) {
                fallProb = if (label.contains("fall")) 0.85f else 0.10f
                Log.w(TAG, "prob=0 fixed → $fallProb (label=$label)")
            }

            val finalLabel = if (label.contains("fall")) "fall" else "safe"
            Log.d(TAG, "✅ Result: label=$finalLabel prob=$fallProb")
            FallResult(label = finalLabel, fallProbability = fallProb)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Inference error: ${e.message}")
            thresholdFallback(window)
        }
    }

    // =========================================================================
    // PROBABILITY PARSE
    // =========================================================================
    @Suppress("UNCHECKED_CAST")
    private fun parseProb(probOutput: Any?, label: String): Float {
        return when (probOutput) {
            is Array<*> -> {
                when (val first = probOutput.firstOrNull()) {
                    is Map<*, *> -> {
                        val fallEntry = first.entries.firstOrNull {
                            it.key.toString().lowercase().contains("fall")
                        }
                        (fallEntry?.value as? Number)?.toFloat()
                            ?: if (label.contains("fall")) 0.85f else 0.10f
                    }
                    is FloatArray  -> if (label.contains("fall")) first.maxOrNull() ?: 0.85f
                    else first.minOrNull() ?: 0.10f
                    is DoubleArray -> if (label.contains("fall")) first.maxOrNull()?.toFloat() ?: 0.85f
                    else first.minOrNull()?.toFloat() ?: 0.10f
                    else -> if (label.contains("fall")) 0.85f else 0.10f
                }
            }
            is FloatArray  -> probOutput.maxOrNull() ?: if (label.contains("fall")) 0.85f else 0.10f
            is DoubleArray -> probOutput.maxOrNull()?.toFloat() ?: if (label.contains("fall")) 0.85f else 0.10f
            else -> if (label.contains("fall")) 0.85f else 0.10f
        }
    }

    // =========================================================================
    // THRESHOLD FALLBACK — used when model not loaded or buffer not full yet
    // =========================================================================
    fun thresholdFallback(window: List<FallSample>): FallResult {
        if (window.isEmpty()) return FallResult("safe", 0.05f)

        val lastSample  = window.last()
        val maxAccMag   = window.maxOf { it.accMag }
        val lastPitch   = abs(lastSample.pitch)

        val impact      = maxAccMag > 14f
        val bigImpact   = maxAccMag > 20f
        val extremeTilt = lastPitch > 50f

        val recentSamples = window.takeLast(5)
        val isStill = recentSamples.isNotEmpty() && recentSamples.all {
            abs(it.gx) < 15f && abs(it.gy) < 15f && abs(it.gz) < 15f
        }

        val isFall = bigImpact ||
                (impact && extremeTilt) ||
                (impact && isStill) ||
                isStill

        val prob = when {
            bigImpact              -> 0.95f
            impact && extremeTilt  -> 0.88f
            impact && isStill      -> 0.82f
            impact                 -> 0.65f
            isStill && extremeTilt -> 0.75f
            isStill                -> 0.60f
            else                   -> 0.05f
        }

        return FallResult(
            label           = if (isFall) "fall" else "safe",
            fallProbability = prob
        )
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================
    fun close() {
        try { session?.close() } catch (_: Exception) {}
        try { env.close()      } catch (_: Exception) {}
    }
}