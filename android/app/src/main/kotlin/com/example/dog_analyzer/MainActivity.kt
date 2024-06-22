package com.example.dog_analyzer

import com.example.dog_analyzer.camera.CameraManager
import com.example.dog_analyzer.camera.DefaultCameraManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.TextureRegistry

class MainActivity : FlutterActivity() {
    companion object {
        private const val METHOD_CHANNEL = "example/dog_analyzer"
        private const val EVENT_CHANNEL = "example/dog_analyzer.stream"
    }

    private lateinit var cameraManager: CameraManager
    private var eventSink: EventChannel.EventSink? = null

    private fun setUpDependencies(textureRegistry: TextureRegistry) {
        cameraManager = DefaultCameraManager(this, textureRegistry)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        setUpDependencies(flutterEngine.renderer)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startCamera" -> {
                    val textureId = cameraManager.startCamera(
                        onData = {
                            activity.runOnUiThread {
                                eventSink?.success(it)
                            }
                        },
                    )
                    result.success(textureId)
                }

                "stopCamera" -> {
                    cameraManager.stopCamera()
                    result.success(true)
                }

                else -> result.notImplemented()
            }
        }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, sink: EventChannel.EventSink?) {
                    eventSink = sink
                }

                override fun onCancel(arguments: Any?) {
                    eventSink = null
                }
            },
        )
    }

}
