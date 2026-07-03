package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SettingCounterRow(
    title: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(

            text = title,

            style = MaterialTheme.typography.bodyLarge,

            modifier = Modifier.weight(1f)

        )

        Row(

            verticalAlignment = Alignment.CenterVertically

        ) {

            CounterButton(
                text = "-",
                onClick = onMinus
            )

            Text(

                text = value.toString(),

                modifier = Modifier.width(50.dp),

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.Center

            )

            CounterButton(
                text = "+",
                onClick = onPlus
            )

        }

    }

}