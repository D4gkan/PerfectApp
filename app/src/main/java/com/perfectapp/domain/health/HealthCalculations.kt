package com.perfectapp.domain.health

/** Pure calculation helpers for body composition. Kept separate from the repository
 *  so they can be unit-tested without touching Room/Flow. */
object HealthCalculations {

    /** A transparent non-medical score: fat loss and retained/gained lean mass improve it. */
    fun recompositionScore(fatMassChangeKg: Double?, muscleMassChangeKg: Double?): Int? {
        if (fatMassChangeKg == null || muscleMassChangeKg == null) return null
        return (50.0 - fatMassChangeKg * 8.0 + muscleMassChangeKg * 12.0).toInt().coerceIn(0, 100)
    }

    fun recompositionLabel(score: Int): String = when (score) {
        in 80..100 -> "Excellent"
        in 60..79 -> "Good"
        in 40..59 -> "Moderate"
        else -> "Needs improvement"
    }

    fun bmi(weightKg: Double, heightCm: Double?): Double? {
        if (heightCm == null || heightCm <= 0) return null
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun fatMassKg(weightKg: Double, bodyFatPercent: Double?): Double? {
        if (bodyFatPercent == null) return null
        return weightKg * (bodyFatPercent / 100.0)
    }

    fun fatFreeMassKg(weightKg: Double, fatMassKg: Double?): Double? {
        if (fatMassKg == null) return null
        return weightKg - fatMassKg
    }

    fun bmiCategory(bmi: Double?): String {
        if (bmi == null) return "--"
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }
}
