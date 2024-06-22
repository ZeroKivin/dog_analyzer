package com.example.dog_analyzer.analyze

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Classifications
import com.google.mediapipe.tasks.components.containers.Detection
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifierResult
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import java.nio.ByteBuffer

typealias DataListener = (Map<String, Any>) -> Unit

class MediapipeDogAnalyzer(
    context: Context,
    private val onData: DataListener,
) : ImageAnalysis.Analyzer {
    private val categoryAllowlist = listOf("dog")

    private val objectDetectorBaseOptions = BaseOptionsBuilder.build("assets\\efficientdet_lite0.tflite")
    private val objectDetectorOptions = ObjectDetector.ObjectDetectorOptions.builder()
        .setCategoryAllowlist(categoryAllowlist)
        .setBaseOptions(objectDetectorBaseOptions)
        .setScoreThreshold(0.5f)
        .setRunningMode(RunningMode.IMAGE)
        .setMaxResults(1)
        .build()
    private val objectDetector = ObjectDetector.createFromOptions(context, objectDetectorOptions)

    private val imageClassifierBaseOptions = BaseOptionsBuilder.build("assets\\dogs_metadata.tflite")
    private val imageClassifierOptions = ImageClassifier.ImageClassifierOptions.builder()
        .setBaseOptions(imageClassifierBaseOptions)
        .setScoreThreshold(0.10f)
        .setRunningMode(RunningMode.IMAGE)
        .setMaxResults(1)
        .build()
    private val imageClassifier = ImageClassifier.createFromOptions(context, imageClassifierOptions)

    private val emptyResult = mapOf(
        "left" to 0f,
        "top" to 0f,
        "right" to 0f,
        "bottom" to 0f,
        "result" to "No dog in sight!",
    )
    private var cachedResult = emptyResult

    override fun analyze(imageProxy: ImageProxy) {
        var result = emptyResult

        val objectDetectorResult = detectObject(imageProxy, objectDetector)

        val detection = objectDetectorResult?.detections()?.maxByOrNull {
            val boundingBox = it.boundingBox()

            (boundingBox.right - boundingBox.left) * (boundingBox.bottom - boundingBox.top)
        }

        if (detection != null) {
            val imageClassifierResult = classifyImage(imageProxy, detection, imageClassifier)

            if (imageClassifierResult != null) {
                val classifications = imageClassifierResult.classificationResult().classifications()

                if (classifications.isNotEmpty()) {
                    result = generateResult(detection, classifications[0])
                }
            }
        }

        if ((result["result"] != cachedResult["result"]) || (result["left"] != cachedResult["left"])) {
            cachedResult = result
            onData(result)
        }

        imageProxy.close()
    }

    private fun detectObject(
        imageProxy: ImageProxy,
        objectDetector: ObjectDetector,
    ): ObjectDetectorResult? {
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        val byteBuffer = imageProxy.planes[0].buffer
        byteBuffer.rewind()
        bitmapBuffer.copyPixelsFromBuffer(byteBuffer)
        val mpImage = BitmapImageBuilder(bitmapBuffer).build()

        return objectDetector.detect(mpImage)
    }

    private fun classifyImage(
        imageProxy: ImageProxy,
        detection: Detection,
        imageClassifier: ImageClassifier,
    ): ImageClassifierResult? {
        val boundingBox = detection.boundingBox()
        val left = boundingBox.left.toInt()
        val top = boundingBox.top.toInt()
        val width = boundingBox.width().toInt()
        val height = boundingBox.height().toInt()
        val size = width * height * 4

        val originalByteBuffer = imageProxy.planes[0].buffer
        val byteBuffer = ByteBuffer.allocateDirect(size)

        try {
            originalByteBuffer.rewind()
            byteBuffer.rewind()
            val byteArray = ByteArray(4)

            for (rowNumber in 0 until height) {
                for (pixelNumber in 0 until width) {
                    val offset = (rowNumber + top) * imageProxy.width * 4 + (left + pixelNumber) * 4
                    byteArray[0] = originalByteBuffer[offset]
                    byteArray[1] = originalByteBuffer[offset + 1]
                    byteArray[2] = originalByteBuffer[offset + 2]
                    byteArray[3] = originalByteBuffer[offset + 3]
                    byteBuffer.put(byteArray)
                }
            }

        } catch (exception: Exception) {
            Log.e("Analyze", "Crop failed", exception)
        }

        byteBuffer.rewind()
        val detectionBitmapBuffer = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888
        )
        detectionBitmapBuffer.copyPixelsFromBuffer(byteBuffer)
        val detectionMPImage = BitmapImageBuilder(detectionBitmapBuffer).build()

        return imageClassifier.classify(detectionMPImage)
    }

    private fun generateResult(
        detection: Detection,
        classifications: Classifications,
    ): Map<String, Any> {
        val stringBuffer = StringBuffer()

        for (category in classifications.categories()) {
            stringBuffer.append(category.categoryName().replaceFirstChar { it.uppercase() })
            stringBuffer.append(" ")
            stringBuffer.append(category.score() * 100)
            stringBuffer.append(" %")
            stringBuffer.append("\n")
        }

        if (stringBuffer.isNotEmpty()) {
            val boundingBox = detection.boundingBox()

            return mapOf(
                "left" to boundingBox.left,
                "top" to boundingBox.top,
                "right" to boundingBox.right,
                "bottom" to boundingBox.bottom,
                "result" to stringBuffer.toString(),
            )
        }

        return emptyResult
    }

    private object BaseOptionsBuilder {
        fun build(modelAssetPath: String): BaseOptions =
            BaseOptions.builder().setDelegate(Delegate.CPU).setModelAssetPath(modelAssetPath).build()
    }
}