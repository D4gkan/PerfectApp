package com.perfectapp.data.repository

import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.BillingCycle
import com.perfectapp.data.entities.ExchangeRateEntity
import com.perfectapp.data.entities.GoldSettingsEntity
import com.perfectapp.data.entities.NetWorthSnapshotEntity
import com.perfectapp.data.entities.SubscriptionEntity
import com.perfectapp.data.entities.TransactionEntity
import com.perfectapp.data.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class WealthRepository(db: PerfectDatabase, context: android.content.Context? = null) {
    private val dao = db.wealthDao()
    private val subscriptionDao = db.subscriptionDao()
    private val scheduler = context?.let { com.perfectapp.domain.notifications.DueDateReminderScheduler(it) }

    val assets: Flow<List<AssetEntity>> = dao.observeAssets()
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val exchangeRates: Flow<List<ExchangeRateEntity>> = dao.observeExchangeRates()
    val goldSettings: Flow<GoldSettingsEntity?> = dao.observeGoldSettings()
    val snapshots: Flow<List<NetWorthSnapshotEntity>> = dao.observeSnapshots()
    val activeSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.observeActive()

    suspend fun upsertAsset(asset: AssetEntity) = dao.upsertAsset(asset)

    /** Updates the native balance only; currency conversion is intentionally display-time only. */
    suspend fun setAssetBalance(asset: AssetEntity, balance: Double) {
        require(balance.isFinite()) { "Asset balance must be a finite number" }
        dao.updateAsset(asset.copy(quantity = balance, updatedAt = java.time.LocalDateTime.now()))
    }
    suspend fun deleteAsset(asset: AssetEntity) = dao.deleteAsset(asset)
    suspend fun addTransaction(transaction: TransactionEntity) = dao.applyTransaction(transaction)
    suspend fun reverseTransaction(transaction: TransactionEntity) = dao.reverseTransaction(transaction)
    suspend fun updateTransaction(previous: TransactionEntity, replacement: TransactionEntity) =
        dao.replaceTransaction(previous, replacement)
    suspend fun setExchangeRate(rate: ExchangeRateEntity) = dao.upsertExchangeRate(rate)
    suspend fun updateGoldSettings(settings: GoldSettingsEntity) = dao.upsertGoldSettings(settings)

    /** Creates the fixed financial dashboard records once, without replacing user balances. */
    suspend fun initializeFinancialDashboard() {
        DEFAULT_ASSETS.forEach { default ->
            if (dao.getAsset(default.id) == null) dao.upsertAsset(default)
        }
        if (dao.observeGoldSettings().first() == null) dao.upsertGoldSettings(GoldSettingsEntity())
        listOf("USD" to 1.0, "EUR" to 0.0, "GBP" to 0.0, "TRY" to 0.0).forEach { (code, rate) ->
            if (exchangeRates.first().none { it.currencyCode == code }) {
                dao.upsertExchangeRate(ExchangeRateEntity(code, rate))
            }
        }
    }
    suspend fun recordSnapshot(date: LocalDate, total: Double) =
        dao.insertSnapshot(NetWorthSnapshotEntity(date = date, totalNetWorth = total))

    fun calculateNetWorth(assets: List<AssetEntity>, rates: Map<String, Double>, baseCurrency: String): Double {
        return assets.sumOf { asset ->
            if (asset.type == com.perfectapp.data.entities.AssetType.GOLD) return@sumOf 0.0
            val rate = if (asset.currencyCode == baseCurrency) 1.0 else (rates[asset.currencyCode] ?: 1.0)
            // Cash balances (including the fixed Gaming Skins balance) are entered in
            // their native currency.  Their unit price is not part of the balance.
            val nativeValue = com.perfectapp.domain.wealth.FinancialMath.nativeAssetValue(asset, GAMING_SKINS_ID)
            val value = nativeValue * rate
            if (asset.isLiability) -value else value
        }
    }

    fun goldValueTry(settings: GoldSettingsEntity): Double = com.perfectapp.domain.wealth.FinancialMath.goldValueTry(
        settings.gramGoldPriceTry,
        listOf(settings.quarterQuantity to QUARTER_GRAMS, settings.halfQuantity to HALF_GRAMS,
            settings.fullQuantity to FULL_GRAMS, settings.republicQuantity to REPUBLIC_GRAMS)
    )

    fun goldValueUsd(settings: GoldSettingsEntity, rates: Map<String, Double>): Double =
        goldValueTry(settings) * (rates["TRY"] ?: 0.0)

    fun netWorthChangeThisMonth(snapshots: List<NetWorthSnapshotEntity>, currentNetWorth: Double): Double? {
        val now = LocalDate.now()
        val lastMonthSnapshot = snapshots
            .filter { it.date.isBefore(now.withDayOfMonth(1)) }
            .maxByOrNull { it.date }
            ?: return null
        return currentNetWorth - lastMonthSnapshot.totalNetWorth
    }

    // --- Subscriptions ---

    suspend fun addSubscription(subscription: SubscriptionEntity): Long = subscriptionDao.upsert(subscription).also { scheduler?.scheduleSubscription(subscription.copy(id = it)) }
    suspend fun updateSubscription(subscription: SubscriptionEntity) = subscriptionDao.update(subscription).also { scheduler?.scheduleSubscription(subscription) }
    suspend fun deleteSubscription(subscription: SubscriptionEntity) = subscriptionDao.delete(subscription).also { scheduler?.cancelSubscription(subscription.id) }
    suspend fun rescheduleSubscriptionReminders() = subscriptionDao.observeAll().first().filter { it.isActive }.forEach { scheduler?.scheduleSubscription(it) }

    fun daysUntilCharge(subscription: SubscriptionEntity): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), subscription.nextChargeDate)

    /** Estimated monthly cost across all active subscriptions, normalizing yearly ones to /12. */
    fun subscriptionMonthlyCost(subscriptions: List<SubscriptionEntity>): Double =
        subscriptions.filter { it.isActive }.sumOf {
            when (it.billingCycle) {
                BillingCycle.MONTHLY -> it.amount
                BillingCycle.YEARLY -> it.amount / 12.0
            }
        }

    /**
     * Auto-charges any active, auto-renewing subscription whose charge date has arrived
     * (or passed, e.g. the app wasn't opened for a while): logs an expense transaction for
     * each cycle that elapsed and advances `nextChargeDate` forward past today. Call this
     * on app open and from the daily background worker so charges never get missed.
     */
    suspend fun processDueSubscriptions() {
        val today = LocalDate.now()
        val subscriptions = subscriptionDao.observeAll().first()

        for (subscription in subscriptions) {
            if (!subscription.isActive || !subscription.autoRenew) continue

            var nextDate = subscription.nextChargeDate
            var cyclesCharged = 0
            val maxCyclesPerRun = 24 // safety cap against runaway loops if untouched for years

            while (!nextDate.isAfter(today) && cyclesCharged < maxCyclesPerRun) {
                // The persisted subscription/date identity makes retrying safe if the process
                // dies after applying a balance change but before rolling the due date forward.
                if (!dao.hasAutomaticSubscriptionCharge(subscription.id, nextDate)) {
                    dao.applyTransaction(
                        TransactionEntity(
                            date = nextDate,
                            type = TransactionType.EXPENSE,
                            category = "Subscription: ${subscription.name}",
                            amount = subscription.amount,
                            currencyCode = subscription.currencyCode,
                            assetId = subscription.fundingAssetId,
                            note = "Auto-renewed",
                            subscriptionId = subscription.id,
                            isAutomatic = true
                        )
                    )
                }
                nextDate = when (subscription.billingCycle) {
                    BillingCycle.MONTHLY -> nextDate.plusMonths(1)
                    BillingCycle.YEARLY -> nextDate.plusYears(1)
                }
                cyclesCharged++
            }

            if (cyclesCharged > 0) {
                subscription.copy(nextChargeDate = nextDate).also { updated -> subscriptionDao.update(updated); scheduler?.scheduleSubscription(updated) }
            }
        }
    }

    companion object {
        const val CASH_USD_ID = 1001L
        const val CASH_EUR_ID = 1002L
        const val CASH_GBP_ID = 1003L
        const val CASH_TRY_ID = 1004L
        const val GAMING_SKINS_ID = 1005L
        const val GOLD_ID = 1006L
        const val QUARTER_GRAMS = 1.75
        const val HALF_GRAMS = 3.5
        const val FULL_GRAMS = 7.0
        const val REPUBLIC_GRAMS = 7.216
        val DEFAULT_ASSETS = listOf(
            AssetEntity(CASH_USD_ID, "Cash (USD)", com.perfectapp.data.entities.AssetType.CASH, "USD", 0.0, 1.0),
            AssetEntity(CASH_EUR_ID, "Cash (EUR)", com.perfectapp.data.entities.AssetType.CASH, "EUR", 0.0, 1.0),
            AssetEntity(CASH_GBP_ID, "Cash (GBP)", com.perfectapp.data.entities.AssetType.CASH, "GBP", 0.0, 1.0),
            AssetEntity(CASH_TRY_ID, "Cash (TRY)", com.perfectapp.data.entities.AssetType.CASH, "TRY", 0.0, 1.0),
            AssetEntity(GAMING_SKINS_ID, "Gaming Skins (USD)", com.perfectapp.data.entities.AssetType.OTHER, "USD", 0.0, 1.0),
            AssetEntity(GOLD_ID, "Gold", com.perfectapp.data.entities.AssetType.GOLD, "TRY", 0.0, 1.0)
        )
    }
}
