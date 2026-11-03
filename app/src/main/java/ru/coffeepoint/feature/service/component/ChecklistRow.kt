package ru.coffeepoint.feature.service.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ChecklistRow(
    checked: Boolean,
    title: String,
    hint: String,
    warningText: String? = null,
    hintStartPadding: Dp = 52.dp,
    hintEmphasis: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {

    var showHint by remember { mutableStateOf(false) }
    val warningColor = Color(0xFFF57C00)
    val hintText = remember(hint, hintEmphasis) {
        buildAnnotatedString {
            append(hint)
            if (!hintEmphasis.isNullOrBlank()) {
                val start = hint.indexOf(hintEmphasis)
                if (start >= 0) {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + hintEmphasis.length)
                }
            }
        }
    }

    Column {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showHint = !showHint
                }
                .padding(vertical = 6.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            Text(
                text = title,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector =
                    if (showHint)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )

        }

        warningText?.let { text ->
            Row(
                modifier = Modifier
                    .padding(
                        start = 0.dp,
                        end = 16.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = warningColor,
                    modifier = Modifier.padding(end = 6.dp)
                )

                Text(
                    text = text,
                    color = warningColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AnimatedVisibility(
            visible = showHint
        ) {

            Text(

                text = hintText,

                modifier = Modifier
                    .padding(
                        start = hintStartPadding,
                        end = 16.dp,
                        bottom = 12.dp
                    ),

                style = MaterialTheme.typography.bodyMedium

            )

        }

    }

}
