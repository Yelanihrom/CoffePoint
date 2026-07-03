package ru.coffeepoint.feature.service.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp

@Composable
fun ChecklistRow(
    checked: Boolean,
    title: String,
    hint: String,
    onCheckedChange: (Boolean) -> Unit
) {

    var showHint by remember { mutableStateOf(false) }

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

            AnimatedVisibility(
                visible = showHint
            ) {

                Text(

                    text = hint,

                    modifier = Modifier
                        .padding(
                            start = 52.dp,
                            end = 16.dp,
                            bottom = 12.dp
                        ),

                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium

                )

            }

        }

    }
