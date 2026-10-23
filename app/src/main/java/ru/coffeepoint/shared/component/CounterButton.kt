package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CounterButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {

    FilledIconButton(

        onClick = onClick,
        enabled = enabled,

        modifier = Modifier.size(30.dp),

        shape = CircleShape

    ) {

        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall
        )

    }

}
