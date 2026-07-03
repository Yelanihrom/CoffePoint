package ru.coffeepoint.feature.station.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SalesProgress(
    sales: Int,
    limit: Int
) {

    val progress = (sales.toFloat() / limit.toFloat()).coerceAtMost(1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(90.dp)
        )

        Text(
            text = "$sales",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "из $limit"
        )

    }

}