package ru.coffeepoint.feature.camera.model

import android.net.Uri

data class ServicePhoto(
    val stationId: Int,
    val photoType: PhotoType,
    val uri: Uri,
    val createdAt: Long = System.currentTimeMillis()
)