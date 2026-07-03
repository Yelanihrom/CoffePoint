package ru.coffeepoint.feature.station.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StockItem(
    title: String,
    value: String
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),

        horizontalArrangement = Arrangement.SpaceBetween

    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(

            text = value,

            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                ),

            style = MaterialTheme.typography.bodyMedium,

            fontWeight = FontWeight.Bold

        )

    }

}