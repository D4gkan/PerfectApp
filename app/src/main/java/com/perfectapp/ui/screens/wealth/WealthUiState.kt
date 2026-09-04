package com.perfectapp.ui.screens.wealth

import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.ExchangeRateEntity
import com.perfectapp.data.entities.GoldSettingsEntity
import com.perfectapp.data.entities.SubscriptionEntity
import com.perfectapp.data.entities.TransactionEntity
import com.perfectapp.domain.wealth.BASE_CURRENCY

data class WealthUiState(
    val isLoading: Boolean = true,
    val assets: List<AssetEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val goldSettings: GoldSettingsEntity = GoldSettingsEntity(),
    val goldValueTry: Double = 0.0,
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val subscriptionMonthlyCost: Double = 0.0,
    val netWorth: Double = 0.0,
    val netWorthChangeThisMonth: Double? = null,
    val baseCurrency: String = BASE_CURRENCY
)
