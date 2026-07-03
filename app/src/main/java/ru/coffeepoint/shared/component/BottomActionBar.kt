package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

        Button(

            onClick = onClick,

            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
                .navigationBarsPadding(),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp
            ),
            enabled = enabled,

            shape = RoundedCornerShape(20.dp)

        ) {

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

        }

    }

}