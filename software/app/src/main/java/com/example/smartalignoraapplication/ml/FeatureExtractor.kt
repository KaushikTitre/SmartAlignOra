package com.example.smartalignoraapplication.ml

import kotlin.math.*

// =============================================================================
// FeatureExtractor — computes the 7-feature vector for posture_model.onnx
//
// Mirrors training.ipynb extract_features() EXACTLY:
//   window  : List of pitch values (20 samples @ 5 Hz = 4 seconds)
//   returns : [mean, std, min, max, range, slope, percent_high]
//
// HIGH_THRESHOLD = 8.0  (matches training: HIGH_THRESHOLD = 8)
// =============================================================================
object FeatureExtractor {

    // Must match the HIGH_THRESHOLD constant in training.ipynb
    private const val HIGH_THRESHOLD = 8f

    /**
     * Compute the 7 posture features from a raw pitch window.
     * @param window  List of pitch degree values (ideally 20 samples)
     * @return FloatArray of size 7: [mean, std, min, max, range, slope, percent_high]
     */
    fun extract(window: List<Float>): FloatArray {

        if (window.isEmpty()) return FloatArray(7)

        // mean
        val mean = window.average().toFloat()

        // std (population std, matching numpy default ddof=0)
        val variance = window.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance).toFloat()

        // min / max / range
        val minVal = window.min()
        val maxVal = window.max()
        val range  = maxVal - minVal

        // slope = last − first  (same as training: window[-1] − window[0])
        val slope = window.last() - window.first()

        // percent_high = fraction of samples where pitch > HIGH_THRESHOLD
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