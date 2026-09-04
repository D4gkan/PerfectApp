package com.perfectapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double, currencyCode: String = "USD"): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        format.currency = java.util.Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        "%.2f %s".format(amount, currencyCode)
    }
}

@Composable
fun CurrencyAmount(
    amount: Double,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium
) {
    Text(text = formatCurrency(amount, currencyCode), style = style, modifier = modifier)
}
