package ru.coffeepoint.feature.camera.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.view.PreviewView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import ru.coffeepoint.feature.camera.capture.CameraCaptureManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission

@Composable
fun CameraScreen(

    onPhotoSaved: (String) -> Unit = {},

    onBack: () -> Unit = {}

) {

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current


    val previewView = remember {
        PreviewView(context)
    }

    val manager = remember(
        context,
        lifecycleOwner,
        previewView
    ) {
        CameraCaptureManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                manager.startCamera()
            }
        }

    LaunchedEffect(Unit) {

        if (
            checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            manager.startCamera()

        } else {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )

        }

    }

    DisposableEffect(manager) {

        onDispose {

            manager.release()

        }

    }

    Scaffold(

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    manager.takePhoto(

                        executor = ContextCompat.getMainExecutor(context),

                        onSuccess = {

                            onPhotoSaved(it)

                        },

                        onError = {

                            it.printStackTrace()

                        }

                    )

                }

            ) {

                Icon(

                    imageVector = Icons.Default.Camera,

                    contentDescription = null

                )

            }

        }

    ) { padding ->

        Box(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {

            AndroidView(

                modifier = Modifier.fillMaxSize(),

                factory = {

                    previewView

                }

            )

        }

    }

}