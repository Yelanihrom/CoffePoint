package ru.coffeepoint.feature.camera.component

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    previewView: PreviewView,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = {
            previewView
        }
    )
}

fun createPreviewView(context: Context): PreviewView {

    return PreviewView(context).apply {

        implementationMode =
            PreviewView.ImplementationMode.COMPATIBLE

        scaleType =
            PreviewView.ScaleType.FILL_CENTER

    }

}