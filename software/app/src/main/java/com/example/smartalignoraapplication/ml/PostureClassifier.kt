package com.example.smartalignoraapplication.ml

import android.content.Context
import ai.onnxruntime.*
import java.nio.FloatBuffer

class PostureClassifier(context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("posture_model.onnx").readBytes()
        session = env.createSession(modelBytes)
    }

    fun classify(features: FloatArray): Int {

        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(features),
            longArrayOf(1, 7)
        )

        val results = session.run(
            mapOf(session.inputNames.iterator().next() to inputTensor)
        )

        val output = results[0].value

        return extractPrediction(output)
    }

    private fun extractPrediction(output: Any?): Int {

        return try {

            when (output) {

                is LongArray -> {
                    output[0].toInt()
                }

                is Array<*> -> {
                    val firstElement = output[0]

                    when (firstElement) {

                        is LongArray -> firstElement[0].toInt()

                        is FloatArray -> {
                            if (firstElement[0] > firstElement[1]) 1 else 0
                        }

                        else -> 0
                    }
                }

                else -> 0
            }

        } catch (e: Exception) {
            0
        }
    }
}
