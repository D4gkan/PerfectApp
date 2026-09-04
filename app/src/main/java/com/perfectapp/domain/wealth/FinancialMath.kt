package com.perfectapp.domain.wealth

import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.AssetType
import com.perfectapp.data.entities.TransactionType

/** Pure financial rules shared by persistence and portfolio valuation. */
object FinancialMath {
    fun transactionDelta(type: TransactionType, amount: Double): Double {
        require(amount.isFinite() && amount > 0.0) { "Transaction amount must be a finite positive number" }
        return if (type == TransactionType.INCOME) amount else -amount
    }

    fun nativeAssetValue(asset: AssetEntity, gamingSkinsId: Long): Double =
        if (asset.type == AssetType.CASH || asset.id == gamingSkinsId) asset.quantity
        else asset.quantity * asset.valuePerUnit

    fun goldValueTry(gramPrice: Double, quantitiesAndWeights: List<Pair<Double, Double>>): Double =
        gramPrice * quantitiesAndWeights.sumOf { (quantity, weight) -> quantity * weight }
}
