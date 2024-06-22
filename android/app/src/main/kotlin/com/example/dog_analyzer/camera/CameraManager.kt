package com.example.dog_analyzer.camera

interface CameraManager {
    fun startCamera(onData: (Map<String, Any>) -> Unit): Long

    fun stopCamera()
}