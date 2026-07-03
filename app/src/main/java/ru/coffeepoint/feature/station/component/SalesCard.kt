package ru.coffeepoint.feature.station.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.coffeepoint.feature.station.model.StationDetail

@Composable
fun SalesCard(
    detail: StationDetail
) {

    val statusColor =
        when {
            detail.station.salesAfterService >= 120 -> Color.Red
            detail.station.salesAfterService >= 100 -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        }

    val statusText =
        when {
            detail.station.salesAfterService >= 120 ->
                "Требует обслуживания"

            detail.station.salesAfterService >= 100 ->
                "Скоро потребуется обслуживание"

            else ->
                "Все в порядке"
        }

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),

        shape = RoundedCornerShape(20.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )

    ) {

        Column(

            modifier = Modifier.padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)

        ) {

            Text(

                text = "Продажи после обслуживания",

                style =
                    MaterialTheme.typography.labelLarge

            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(

                text =
                    "${detail.station.salesAfterService} / ${detail.station.salesLimit}",

                style =
                    MaterialTheme.typography.displaySmall,

                fontWeight = FontWeight.Bold

            )

            Text(

                text = statusText,

                color = statusColor,

                fontWeight = FontWeight.Bold

            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(

                text = "Последнее обслуживание",

                style =
                    MaterialTheme.typography.labelLarge

            )

            Text(
                text = detail.station.lastServiceDate
            )

        }

    }

}