package com.example.dog_analyzer.camera.surface_provider

import android.app.Activity
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat
import io.flutter.view.TextureRegistry

class DefaultSurfaceProvider(
    private val activity: Activity,
    private val textureEntry: TextureRegistry.SurfaceTextureEntry,
) : Preview.SurfaceProvider {
    override fun onSurfaceRequested(request: SurfaceRequest) {
        val resolution = request.resolution
        val texture = textureEntry.surfaceTexture()
        texture.setDefaultBufferSize(resolution.width, resolution.height)
        val surface = Surface(texture)
        val executor = ContextCompat.getMainExecutor(activity)
        request.provideSurface(surface, executor) { }
    }
}