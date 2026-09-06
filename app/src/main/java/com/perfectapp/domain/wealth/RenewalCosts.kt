package com.perfectapp.domain.wealth

import com.perfectapp.data.entities.ReminderEntity
import com.perfectapp.data.entities.SubscriptionEntity
import com.perfectapp.data.entities.BillingCycle

data class RenewalCostTotal(val usd: Double, val missingCurrencies: Set<String>)
object RenewalCosts {
    fun usd(amount: Double, currency: String, rates: Map<String, Double>): Double? {
        val code = currency.trim().uppercase()
        val rate = if (code == "USD") 1.0 else rates[code]
        return rate?.takeIf { it.isFinite() && it > 0 }?.let { amount * it }
    }
    fun monthly(reminders: List<ReminderEntity>, subscriptions: List<SubscriptionEntity>, rates: Map<String, Double>): RenewalCostTotal {
        val amounts = reminders.filter { !it.isCompleted && it.isRecurring && it.amount != null && (it.recurrenceMonths ?: 0) > 0 }
            .map { (it.amount!! / it.recurrenceMonths!!) to (it.currencyCode ?: "USD") } +
            subscriptions.filter { it.isActive && it.autoRenew }.map { (it.amount / if (it.billingCycle == BillingCycle.YEARLY) 12 else 1) to it.currencyCode }
        val missing = amounts.filter { usd(it.first, it.second, rates) == null }.map { it.second }.toSet()
        return RenewalCostTotal(amounts.sumOf { usd(it.first, it.second, rates) ?: 0.0 }, missing)
    }
}
