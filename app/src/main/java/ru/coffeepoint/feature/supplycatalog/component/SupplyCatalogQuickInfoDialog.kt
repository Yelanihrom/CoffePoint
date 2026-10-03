package ru.coffeepoint.feature.supplycatalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.feature.supplycatalog.model.SupplyCatalogItemType
import ru.coffeepoint.shared.component.asActionButtonText

@Composable
fun SupplyCatalogQuickInfoDialog(
    itemType: String,
    quantityText: String?,
    items: List<SupplyCatalogEntity>,
    onDismiss: () -> Unit
) {
    val title = SupplyCatalogItemType.labelOf(itemType)
    val options = items
        .filter { item ->
            item.itemType == itemType && item.isActive
        }
        .sortedWith(
            compareBy<SupplyCatalogEntity> { item -> item.priority }
                .thenBy { item -> item.id }
        )

    AlertDialog(
        modifier = Modifier.widthIn(min = 320.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!quantityText.isNullOrBlank()) {
                    Text(
                        text = "\u041d\u0443\u0436\u043d\u043e \u043a\u0443\u043f\u0438\u0442\u044c: $quantityText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (options.isEmpty()) {
                    Text(
                        text = "\u0412\u0430\u0440\u0438\u0430\u043d\u0442\u044b \u0437\u0430\u043a\u0443\u043f\u043a\u0438 \u043d\u0435 \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u044b",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    options.forEach { item ->
                        QuickInfoOptionCard(item)
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c".asActionButtonText())
                }
            }
        }
    )
}

@Composable
private fun QuickInfoOptionCard(
    item: SupplyCatalogEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "\u0410\u0440\u0442\u0438\u043a\u0443\u043b: ${item.article.ifBlank { "\u2014" }}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "${priceTitleFor(item.itemType)}: \u0434\u043e ${item.maxPrice} \u0440",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun priceTitleFor(itemType: String): String {
    return when (itemType) {
        SupplyCatalogItemType.Cups.code,
        SupplyCatalogItemType.Lids.code,
        SupplyCatalogItemType.Sugar.code,
        SupplyCatalogItemType.Stirrers.code,
        SupplyCatalogItemType.GarbageBags.code -> "\u0426\u0435\u043d\u0430 \u0437\u0430 \u0448\u0442"

        else -> "\u0426\u0435\u043d\u0430 \u0437\u0430 \u0443\u043f"
    }
}
