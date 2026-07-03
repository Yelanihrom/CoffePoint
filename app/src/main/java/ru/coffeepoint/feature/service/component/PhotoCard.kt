package ru.coffeepoint.feature.service.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.coffeepoint.feature.camera.model.ServicePhoto

@Composable
fun PhotoCard(
    machinePhoto: ServicePhoto?,
    overviewPhoto: ServicePhoto?,
    onMachineClick: () -> Unit,
    onOverviewClick: () -> Unit
) {

    Card {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Фотографии",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = onMachineClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Фото открытого аппарата")
            }

            machinePhoto?.let {

                AsyncImage(
                    model = it.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOverviewClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Общее фото стойки")
            }

            overviewPhoto?.let {

                AsyncImage(
                    model = it.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )

            }

        }

    }

}