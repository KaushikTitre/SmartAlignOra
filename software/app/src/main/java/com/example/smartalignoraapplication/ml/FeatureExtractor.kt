package com.example.smartalignoraapplication.ml

import kotlin.math.*

object FeatureExtractor {

    private const val HIGH_THRESHOLD = 8f

    fun extract(window: List<Float>): FloatArray {

        if (window.isEmpty()) return FloatArray(7)

        val mean = window.average().toFloat()

        val variance = window.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance).toFloat()

        val minVal = window.minOrNull() ?: 0f
        val maxVal = window.maxOrNull() ?: 0f

        val range = maxVal - minVal

        val slope = window.last() - window.first()

        val percentHigh =
            window.count { it > HIGH_THRESHOLD }.toFloat() / window.size

        return floatArrayOf(
            mean,
            std,
            minVal,
            maxVal,
            range,
            slope,
            percentHigh
        )
    }
}
