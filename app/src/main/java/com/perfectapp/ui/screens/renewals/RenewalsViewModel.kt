package com.perfectapp.ui.screens.renewals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.entities.ReminderEntity
import com.perfectapp.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RenewalsViewModel(private val repository: ReminderRepository) : ViewModel() {

    val uiState: StateFlow<RenewalsUiState> = repository.all
        .map { items ->
            RenewalsUiState(
                isLoading = false,
                items = items,
                estimatedMonthlyCost = repository.estimatedMonthlyCost(items)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RenewalsUiState()
        )

    fun markPaid(reminder: ReminderEntity) {
        viewModelScope.launch { repository.markPaid(reminder) }
    }

    fun delete(reminder: ReminderEntity) {
        viewModelScope.launch { repository.delete(reminder) }
    }
}
