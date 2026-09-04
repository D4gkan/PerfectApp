package com.perfectapp.ui.screens.renewals

import com.perfectapp.data.entities.ReminderEntity

data class RenewalsUiState(
    val isLoading: Boolean = true,
    val items: List<ReminderEntity> = emptyList(),
    val estimatedMonthlyCost: Double = 0.0
)
