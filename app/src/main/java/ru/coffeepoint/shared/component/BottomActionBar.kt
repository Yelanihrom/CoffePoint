package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionBar(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {

    BottomAppBar(

        containerColor = MaterialTheme.colorScheme.surface

    ) {

        PrimaryActionButton(
            text = text,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 12.dp
                )
                .navigationBarsPadding(),
            enabled = enabled,
            textStyle = MaterialTheme.typography.titleMedium
        )

    }

}
