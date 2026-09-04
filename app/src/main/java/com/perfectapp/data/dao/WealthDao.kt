package com.perfectapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.ExchangeRateEntity
import com.perfectapp.data.entities.GoldSettingsEntity
import com.perfectapp.data.entities.NetWorthSnapshotEntity
import com.perfectapp.data.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: AssetEntity): Long

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :assetId LIMIT 1")
    suspend fun getAsset(assetId: Long): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAsset(asset: AssetEntity): Long

    @Query("SELECT * FROM assets ORDER BY updatedAt DESC")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    /** Records an income/expense and changes its selected asset atomically. */
    @Transaction
    suspend fun applyTransaction(transaction: TransactionEntity): Long {
        adjustAssetFor(transaction, reverse = false)
        return insertTransaction(transaction)
    }

    /** Removes a transaction and restores the balance it originally changed. */
    @Transaction
    suspend fun reverseTransaction(transaction: TransactionEntity) {
        adjustAssetFor(transaction, reverse = true)
        deleteTransaction(transaction)
    }

    /** Replaces a transaction without leaving either its former or new asset out of balance. */
    @Transaction
    suspend fun replaceTransaction(previous: TransactionEntity, replacement: TransactionEntity) {
        require(previous.id == replacement.id) { "A transaction can only replace itself" }
        adjustAssetFor(previous, reverse = true)
        adjustAssetFor(replacement, reverse = false)
        updateTransaction(replacement)
    }

    private suspend fun adjustAssetFor(transaction: TransactionEntity, reverse: Boolean) {
        val asset = transaction.assetId?.let { getAsset(it) }
        require(asset != null) { "The selected asset no longer exists" }
        require(asset.isActive) { "The selected asset is inactive" }
        require(asset.currencyCode.equals(transaction.currencyCode, ignoreCase = true)) {
            "Transaction currency must match its selected asset"
        }
        // Amounts are native-currency balances. Dividing by valuePerUnit caused the crash
        // for valid cash assets initialized with a zero price.
        val signedAmount = com.perfectapp.domain.wealth.FinancialMath.transactionDelta(transaction.type, transaction.amount) * if (reverse) -1 else 1
        updateAsset(asset.copy(quantity = asset.quantity + signedAmount, updatedAt = java.time.LocalDateTime.now()))
    }

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE subscriptionId = :subscriptionId AND date = :date AND isAutomatic = 1)")
    suspend fun hasAutomaticSubscriptionCharge(subscriptionId: Long, date: java.time.LocalDate): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExchangeRate(rate: ExchangeRateEntity)

    @Query("SELECT * FROM exchange_rates")
    fun observeExchangeRates(): Flow<List<ExchangeRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoldSettings(settings: GoldSettingsEntity)

    @Query("SELECT * FROM gold_settings WHERE id = 1")
    fun observeGoldSettings(): Flow<GoldSettingsEntity?>

    @Insert
    suspend fun insertSnapshot(snapshot: NetWorthSnapshotEntity): Long

    @Query("SELECT * FROM net_worth_snapshots ORDER BY date DESC")
    fun observeSnapshots(): Flow<List<NetWorthSnapshotEntity>>
}
