package ru.coffeepoint.feature.service.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PhotoSection(
    machinePhotoPath: String?,
    overviewPhotoPath: String?
) {

    Column(

        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {
        PhotoCard(
            title = "Фото открытого аппарата",
            photoPath = machinePhotoPath
        )

        PhotoCard(
            title = "Общее фото стойки",
            photoPath = overviewPhotoPath
        )

    }

}

@Composable
private fun PhotoCard(
    title: String,
    photoPath: String?
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(

            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),

            shape = RoundedCornerShape(24.dp)

        ) {

            Box(

                modifier = Modifier
                    .fillMaxSize(),

                contentAlignment = Alignment.Center

            ) {

                if (photoPath.isNullOrBlank()) {



                    Column(

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.spacedBy(12.dp)

                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "Фото отсутствует",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "После обслуживания здесь появится фотография",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                }
                else {

                    AsyncImage(

                        model = File(photoPath),

                        contentDescription = title,

                        modifier = Modifier.fillMaxSize(),

                        contentScale = ContentScale.Crop

                    )

                }

            }

        }

    }

}