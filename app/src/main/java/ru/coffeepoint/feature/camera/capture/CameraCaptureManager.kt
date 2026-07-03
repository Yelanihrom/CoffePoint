package ru.coffeepoint.feature.camera.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import ru.coffeepoint.feature.camera.PhotoManager
import java.util.concurrent.Executor
import androidx.lifecycle.LifecycleOwner

class CameraCaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            val cameraProvider = cameraProvider!!

            val preview =
                Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider =
                            previewView.surfaceProvider
                    }

            imageCapture =
                ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .build()

            val selector =
                CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(
        executor: Executor,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {

        val capture = imageCapture ?: return

        val file =
            PhotoManager.createPhotoFile(context)

        val output =
            ImageCapture.OutputFileOptions.Builder(file)
                .build()

        capture.takePicture(
            output,
            executor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    onSuccess(file.absolutePath)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    onError(exception)
                }

            }
        )
    }
    fun release() {

        cameraProvider?.unbindAll()

        imageCapture = null

    }
}