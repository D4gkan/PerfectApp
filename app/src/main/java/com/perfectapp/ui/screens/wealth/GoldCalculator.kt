package com.perfectapp.ui.screens.wealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.perfectapp.data.entities.GoldSettingsEntity
import com.perfectapp.data.repository.WealthRepository
import com.perfectapp.ui.components.PremiumCard
import com.perfectapp.ui.components.formatCurrency

@Composable
fun GoldCalculator(settings: GoldSettingsEntity, tryToUsd: Double, onChange: (GoldSettingsEntity) -> Unit) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField("Gram Gold Price (TRY)", settings.gramGoldPriceTry) { onChange(settings.copy(gramGoldPriceTry = it)) }
            GoldRow("Quarter Gold (Çeyrek)", WealthRepository.QUARTER_GRAMS, settings.quarterQuantity, settings.gramGoldPriceTry) { onChange(settings.copy(quarterQuantity = it)) }
            GoldRow("Half Gold (Yarım)", WealthRepository.HALF_GRAMS, settings.halfQuantity, settings.gramGoldPriceTry) { onChange(settings.copy(halfQuantity = it)) }
            GoldRow("Full Gold (Tam)", WealthRepository.FULL_GRAMS, settings.fullQuantity, settings.gramGoldPriceTry) { onChange(settings.copy(fullQuantity = it)) }
            GoldRow("Republic Gold (Cumhuriyet)", WealthRepository.REPUBLIC_GRAMS, settings.republicQuantity, settings.gramGoldPriceTry) { onChange(settings.copy(republicQuantity = it)) }
            val total = settings.gramGoldPriceTry * (settings.quarterQuantity * WealthRepository.QUARTER_GRAMS + settings.halfQuantity * WealthRepository.HALF_GRAMS + settings.fullQuantity * WealthRepository.FULL_GRAMS + settings.republicQuantity * WealthRepository.REPUBLIC_GRAMS)
            Text("Total Gold Value: ${formatCurrency(total, "TRY")}", style = MaterialTheme.typography.titleMedium)
            Text("≈ ${formatCurrency(total * tryToUsd, "USD")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun GoldRow(name: String, grams: Double, quantity: Double, gramPrice: Double, onChange: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$name · $grams g each", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField("Quantity", quantity, Modifier.weight(1f), onChange)
            Text(formatCurrency(quantity * grams * gramPrice, "TRY"), modifier = Modifier.weight(1f))
        }
    }
}

@Composable private fun NumberField(label: String, value: Double, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value == 0.0) "" else value.toString()) }
    OutlinedTextField(text, { entered -> text = entered; entered.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }?.let(onChange) }, label = { Text(label) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier)
}
