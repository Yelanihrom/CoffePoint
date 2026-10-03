package ru.coffeepoint.feature.station.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StockItem(
    title: String,
    value: String,
    @DrawableRes iconRes: Int? = null,
    onClick: () -> Unit = {}
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),

        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically

    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            iconRes?.let { resource ->
                Image(
                    painter = painterResource(id = resource),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(26.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }

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
