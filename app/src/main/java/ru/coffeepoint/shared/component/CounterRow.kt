package ru.coffeepoint.shared.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
fun CounterRow(
    title: String,
    value: Int,
    max: Int,
    showMax: Boolean = true,
    valueSuffix: String = "",
    @DrawableRes iconRes: Int? = null,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    valueColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {

    Row(

        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),

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

        Row(

            verticalAlignment = Alignment.CenterVertically

        ) {

            CounterButton(
                text = "-",
                onClick = onMinus,
                enabled = enabled
            )

            Text(

                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = valueColor)) { append(value.toString()) }
                    if (valueSuffix.isNotBlank()) append(" $valueSuffix")
                    if (showMax) {
                        append(" / $max")
                        if (valueSuffix.isNotBlank()) append(" $valueSuffix")
                    }
                },

                modifier = Modifier.width(96.dp),

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.Center

            )

            CounterButton(
                text = "+",
                onClick = onPlus,
                enabled = enabled
            )

        }

    }

}
