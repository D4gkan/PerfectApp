package com.perfectapp.domain.wealth

import com.perfectapp.data.entities.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RenewalCostsTest {
    @Test fun tryAmountIsConvertedRatherThanRelabeled() {
        val reminder = ReminderEntity(title = "Test", dueDate = LocalDate.of(2026, 10, 1), amount = 55.0, currencyCode = "TRY", isRecurring = true, recurrenceMonths = 1)
        val total = RenewalCosts.monthly(listOf(reminder), emptyList(), mapOf("TRY" to 0.025))
        assertEquals(1.375, total.usd, 0.00001)
        assertTrue(total.missingCurrencies.isEmpty())
        assertEquals("TRY", reminder.currencyCode)
    }
    @Test fun missingOrZeroRatesAreNotAssumedToBeUsd() {
        assertNull(RenewalCosts.usd(55.0, "TRY", emptyMap()))
        assertNull(RenewalCosts.usd(55.0, "TRY", mapOf("TRY" to 0.0)))
        assertNull(RenewalCosts.usd(55.0, "TRY", mapOf("TRY" to Double.NaN)))
    }
    @Test fun yearlyAndMonthlyRenewalsUseRatesAndExcludeCompletedOrPaused() {
        val reminder = ReminderEntity(title = "Annual", dueDate = LocalDate.of(2026, 10, 1), amount = 120.0, currencyCode = "EUR", isRecurring = true, recurrenceMonths = 12)
        val subscription = SubscriptionEntity(name = "Monthly", amount = 5.0, currencyCode = "USD", billingCycle = BillingCycle.MONTHLY, nextChargeDate = reminder.dueDate)
        val total = RenewalCosts.monthly(listOf(reminder, reminder.copy(isCompleted = true)), listOf(subscription, subscription.copy(autoRenew = false)), mapOf("EUR" to 1.1))
        assertEquals(16.0, total.usd, 0.00001)
    }
    @Test fun incompleteTotalsIdentifyMissingCurrencies() {
        val reminder = ReminderEntity(title = "Test", dueDate = LocalDate.of(2026, 10, 1), amount = 55.0, currencyCode = "TRY", isRecurring = true, recurrenceMonths = 1)
        assertEquals(setOf("TRY"), RenewalCosts.monthly(listOf(reminder), emptyList(), emptyMap()).missingCurrencies)
    }
}
