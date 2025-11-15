
package com.example.edgedetect

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.opengl.GLSurfaceView

class MainActivity : Activity() {
    private lateinit var textureView: TextureView
    private lateinit var fpsText: TextView
    private lateinit var toggleBtn: Button
    private lateinit var cameraController: CameraController
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: GLSurfaceRenderer

    private var showEdges = true

    companion object {
        init { System.loadLibrary("native-lib") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.previewTexture)
        fpsText = findViewById(R.id.fpsText)
        toggleBtn = findViewById(R.id.toggleBtn)
        glView = findViewById(R.id.glView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        // Setup GLSurfaceView and renderer
        glView.setEGLContextClientVersion(2)
        renderer = GLSurfaceRenderer(this)
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        cameraController = CameraController(this) { width, height, buffer ->
            val start = System.nanoTime()
            val out = processFrame(buffer, width, height, if (showEdges) 1 else 0)
            val dt = (System.nanoTime() - start) / 1_000_000.0
            // Queue renderer update on GL thread
            glView.queueEvent {
                renderer.updateFrame(out, width, height)
            }
            runOnUiThread { fpsText.text = "frame time: ${"%.1f".format(dt)} ms" }
        }

        toggleBtn.setOnClickListener { showEdges = !showEdges }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                // Start camera preview (preview surface used but is hidden)
                cameraController.startPreview(surface, w, h)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                cameraController.stopPreview()
                return true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        // If texture view already available, start immediately
        if (textureView.isAvailable) {
            textureView.surfaceTextureListener?.onSurfaceTextureAvailable(textureView.surfaceTexture!!, textureView.width, textureView.height)
        }
    }

    external fun processFrame(inputNV21: ByteArray, width: Int, height: Int, mode: Int): ByteArray
}
