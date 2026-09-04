package com.perfectapp.domain.wealth

import com.perfectapp.data.entities.AssetEntity
import com.perfectapp.data.entities.AssetType
import com.perfectapp.data.entities.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialMathTest {
    @Test fun income_and_expense_use_native_decimal_amounts() {
        assertEquals(12.75, FinancialMath.transactionDelta(TransactionType.INCOME, 12.75), 0.0)
        assertEquals(-12.75, FinancialMath.transactionDelta(TransactionType.EXPENSE, 12.75), 0.0)
    }

    @Test fun cash_balances_are_not_multiplied_by_a_unit_price() {
        val eur = AssetEntity(1002, "Cash (EUR)", AssetType.CASH, "EUR", 45.5, 0.0)
        assertEquals(45.5, FinancialMath.nativeAssetValue(eur, 1005), 0.0)
    }

    @Test fun gold_value_uses_quantities_weights_and_try_price() {
        assertEquals(2175.0, FinancialMath.goldValueTry(1000.0, listOf(1.0 to 1.75, 1.0 to 0.425)), 0.0)
    }
}
