package com.perfectapp.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.entities.BodyMeasurementEntity
import com.perfectapp.data.entities.BodySegment
import com.perfectapp.data.entities.SegmentalCompositionEntity
import com.perfectapp.data.repository.HealthRepository
import com.perfectapp.domain.health.HealthCalculations
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class SegmentInput(
    val fatPercent: Double? = null,
    val fatMassKg: Double? = null,
    val ffmKg: Double? = null,
    val predictedMuscleMassKg: Double? = null,
    val impedance: Double? = null
)

class AddMeasurementViewModel(private val repository: HealthRepository) : ViewModel() {

    /**
     * Saves a measurement, computing BMI / fat mass / fat-free mass from the raw inputs,
     * then optionally persists per-segment readings if the user filled any in.
     */
    fun saveMeasurement(
        weightKg: Double,
        measurementDate: LocalDate,
        measurementTime: LocalTime,
        bodyType: String?,
        gender: String?,
        age: Int?,
        heightCm: Double?,
        bodyFatPercent: Double?,
        manualFatMassKg: Double?,
        manualFfmKg: Double?,
        visceralFatRating: Double?,
        wholeBodyImpedance: Double?,
        segmentEntries: Map<BodySegment, SegmentInput>,
        onSaved: () -> Unit
    ) {
        val bmi = HealthCalculations.bmi(weightKg, heightCm)
        val fatMassKg = manualFatMassKg ?: HealthCalculations.fatMassKg(weightKg, bodyFatPercent)
        val ffmKg = manualFfmKg ?: HealthCalculations.fatFreeMassKg(weightKg, fatMassKg)

        val measurement = BodyMeasurementEntity(
            measurementDate = measurementDate,
            measurementTime = measurementTime,
            bodyType = bodyType,
            gender = gender,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            bmi = bmi,
            bodyFatPercent = bodyFatPercent,
            fatMassKg = fatMassKg,
            ffmKg = ffmKg,
            visceralFatRating = visceralFatRating,
            wholeBodyImpedance = wholeBodyImpedance
        )

        viewModelScope.launch {
            val measurementId = repository.saveMeasurement(measurement)
            segmentEntries.forEach { (segment, values) ->
                if (listOf(values.fatPercent, values.fatMassKg, values.ffmKg, values.predictedMuscleMassKg, values.impedance).any { it != null }) {
                    repository.saveSegment(
                        SegmentalCompositionEntity(
                            measurementId = measurementId,
                            segment = segment,
                            fatPercent = values.fatPercent,
                            fatMassKg = values.fatMassKg,
                            ffmKg = values.ffmKg,
                            predictedMuscleMassKg = values.predictedMuscleMassKg,
                            impedance = values.impedance
                        )
                    )
                }
            }
            onSaved()
        }
    }
}
