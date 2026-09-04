package com.perfectapp.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthCalculationsTest {
    @Test fun bmi_uses_metric_formula() {
        assertEquals(24.22, HealthCalculations.bmi(70.0, 170.0)!!, 0.01)
    }

    @Test fun bmi_requires_valid_height() {
        assertNull(HealthCalculations.bmi(70.0, null))
        assertNull(HealthCalculations.bmi(70.0, 0.0))
    }

    @Test fun composition_mass_is_derived_consistently() {
        val fatMass = HealthCalculations.fatMassKg(80.0, 25.0)
        assertEquals(20.0, fatMass!!, 0.001)
        assertEquals(60.0, HealthCalculations.fatFreeMassKg(80.0, fatMass)!!, 0.001)
    }
}
