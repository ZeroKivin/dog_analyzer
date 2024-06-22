package com.example.dog_analyzer.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dog_analyzer.analyzer.MediapipeDogAnalyzer
import com.example.dog_analyzer.camera.surface_provider.DefaultSurfaceProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.view.TextureRegistry
import java.util.concurrent.Executors

private const val CAMERA_PERMISSION_REQUEST_CODE = 1001

class DefaultCameraManager(
    private val activity: FlutterActivity,
    private val textureRegistry: TextureRegistry,
) : CameraManager {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var textureEntry: TextureRegistry.SurfaceTextureEntry? = null

    private val textureEntryRequired: TextureRegistry.SurfaceTextureEntry
        get() {
            if (textureEntry == null) {
                textureEntry = textureRegistry.createSurfaceTexture()
            }

            return textureEntry!!
        }

    override fun startCamera(
        onData: (Map<String, Any>) -> Unit,
    ): Long {
        if (!checkCameraPermission(activity)) {
            requestCameraPermission(activity)
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        val textureId = textureEntryRequired.id()

        cameraProviderFuture.addListener({
            val surfaceProvider = DefaultSurfaceProvider(activity, textureEntryRequired)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }

            val analyzer = MediapipeDogAnalyzer(
                activity,
                onData = onData,
            )
            val imageAnalysis = ImageAnalysis.Builder().setOutputImageRotationEnabled(true)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            } catch (exception: Exception) {
                Log.e("DefaultCameraManager", "Use case binding failed", exception)
            }
        }, ContextCompat.getMainExecutor(activity))

        return textureId
    }

    override fun stopCamera() {
        textureEntry?.release()
        textureEntry = null
    }

    private fun requestCameraPermission(activity: Activity) = ActivityCompat.requestPermissions(
        activity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
    )

    private fun checkCameraPermission(activity: Activity): Boolean = ContextCompat.checkSelfPermission(
        activity, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}