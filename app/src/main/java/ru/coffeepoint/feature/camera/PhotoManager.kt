package ru.coffeepoint.feature.camera

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoManager {

    fun createPhotoFile(context: Context): File {

        val dir = File(
            context.filesDir,
            "photos"
        )

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val name = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        return File(
            dir,
            "$name.jpg"
        )
    }

}