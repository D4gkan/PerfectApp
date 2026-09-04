package com.perfectapp.ui.screens.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.ExchangeRateEntity
import com.perfectapp.data.entities.GoldSettingsEntity
import com.perfectapp.data.entities.SubscriptionEntity
import com.perfectapp.data.entities.TransactionEntity
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.domain.wealth.BASE_CURRENCY
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WealthViewModel(private val repository: WealthRepository) : ViewModel() {

    private data class WealthInputs(
        val assets: List<AssetEntity>,
        val transactions: List<TransactionEntity>,
        val rates: List<ExchangeRateEntity>,
        val snapshots: List<com.perfectapp.data.entities.NetWorthSnapshotEntity>,
        val subscriptions: List<SubscriptionEntity>
    )

    init {
        // Auto-charge any subscriptions whose renewal date has arrived every time this
        // screen is opened, so the daily background worker isn't the only thing keeping
        // balances and transaction history accurate.
        viewModelScope.launch {
            repository.initializeFinancialDashboard()
            repository.processDueSubscriptions()
        }
    }

    val uiState: StateFlow<WealthUiState> = combine(
        repository.assets,
        repository.transactions,
        repository.exchangeRates,
        repository.snapshots,
        repository.activeSubscriptions
    ) { assets, transactions, rates, snapshots, subscriptions ->
        WealthInputs(assets, transactions, rates, snapshots, subscriptions)
    }.combine(repository.goldSettings) { inputs, persistedGold ->
        val rateMap = inputs.rates.associate { it.currencyCode to it.rateToBase }
        val gold = persistedGold ?: GoldSettingsEntity()
        val netWorth = repository.calculateNetWorth(inputs.assets, rateMap, BASE_CURRENCY) + repository.goldValueUsd(gold, rateMap)
        WealthUiState(
            isLoading = false,
            assets = inputs.assets,
            transactions = inputs.transactions,
            exchangeRates = inputs.rates,
            goldSettings = gold,
            goldValueTry = repository.goldValueTry(gold),
            subscriptions = inputs.subscriptions,
            subscriptionMonthlyCost = repository.subscriptionMonthlyCost(inputs.subscriptions),
            netWorth = netWorth,
            netWorthChangeThisMonth = repository.netWorthChangeThisMonth(inputs.snapshots, netWorth),
            baseCurrency = BASE_CURRENCY
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WealthUiState()
    )

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch { repository.deleteAsset(asset) }
    }

    fun setAssetBalance(asset: AssetEntity, balance: Double) {
        if (!balance.isFinite()) return
        viewModelScope.launch { repository.setAssetBalance(asset, balance) }
    }

    fun setExchangeRate(currencyCode: String, rateToBase: Double) {
        if (!rateToBase.isFinite() || rateToBase < 0.0 || currencyCode !in setOf("USD", "EUR", "GBP", "TRY")) return
        viewModelScope.launch {
            repository.setExchangeRate(ExchangeRateEntity(currencyCode = currencyCode, rateToBase = if (currencyCode == "USD") 1.0 else rateToBase))
        }
    }

    fun updateGoldSettings(settings: GoldSettingsEntity) {
        if (listOf(settings.gramGoldPriceTry, settings.quarterQuantity, settings.halfQuantity, settings.fullQuantity, settings.republicQuantity).all { it.isFinite() && it >= 0.0 }) {
            viewModelScope.launch { repository.updateGoldSettings(settings) }
        }
    }

    fun recordSnapshot() {
        viewModelScope.launch {
            repository.recordSnapshot(LocalDate.now(), uiState.value.netWorth)
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch { repository.deleteSubscription(subscription) }
    }

    fun reverseTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { repository.reverseTransaction(transaction) }
    }

    fun pauseSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch { repository.updateSubscription(subscription.copy(autoRenew = false)) }
    }

    fun resumeSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch { repository.updateSubscription(subscription.copy(autoRenew = true)) }
    }
}
