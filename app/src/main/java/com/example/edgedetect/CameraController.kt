
package com.example.edgedetect

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import kotlin.math.max

/**
 * Camera2 controller that opens the back camera, configures an ImageReader in YUV_420_888
 * and delivers frames as NV21 in the provided callback (width, height, nv21ByteArray).
 */
class CameraController(private val activity: Activity, private val onFrame: (Int, Int, ByteArray) -> Unit) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var previewSize: Size = Size(1280, 720)

    @SuppressLint("MissingPermission")
    fun startPreview(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        startBackgroundThread()
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList.first { id ->
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            }

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888) ?: arrayOf(previewSize)
            // choose a reasonable size (prefer largest available)
            previewSize = sizes.sortedWith(compareBy { max(it.width, it.height) }).lastOrNull() ?: previewSize

            reader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 3).apply {
                setOnImageAvailableListener({ r ->
                    val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val nv21 = yuv420ToNV21(img)
                    onFrame(img.width, img.height, nv21)
                    img.close()
                }, backgroundHandler)
            }

            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(surfaceTexture)

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    try {
                        val targets = listOf(previewSurface, reader!!.surface)
                        camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                captureSession = session
                                val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                    addTarget(previewSurface)
                                    addTarget(reader!!.surface)
                                    set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                                }
                                session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }, backgroundHandler)
                    } catch (e: Exception) { e.printStackTrace() }
                }

                override fun onDisconnected(camera: CameraDevice) { camera.close(); cameraDevice = null }
                override fun onError(camera: CameraDevice, error: Int) { camera.close(); cameraDevice = null }
            }, backgroundHandler)

        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopPreview() {
        captureSession?.close(); captureSession = null
        cameraDevice?.close(); cameraDevice = null
        reader?.close(); reader = null
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely();
        try { backgroundThread?.join(); backgroundThread = null; backgroundHandler = null } catch (e: InterruptedException) { e.printStackTrace() }
    }

    private fun yuv420ToNV21(image: Image): ByteArray {
        // Convert YUV_420_888 to NV21 (semi-planar) which is what many native pipelines expect
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U (Cb)
        val vBuffer = image.planes[2].buffer // V (Cr)

        yBuffer.get(nv21, 0, ySize)

        val pixelStride = image.planes[2].pixelStride
        val rowStride = image.planes[2].rowStride
        val vRow = ByteArray(rowStride)
        val uRow = ByteArray(rowStride)

        // Interleave V and U to NV21 format: VU VU VU...
        var offset = ySize
        for (row in 0 until height / 2) {
            vBuffer.position(row * rowStride)
            uBuffer.position(row * rowStride)
            vBuffer.get(vRow, 0, rowStride)
            uBuffer.get(uRow, 0, rowStride)
            var col = 0
            while (col < width) {
                nv21[offset++] = vRow[col]
                nv21[offset++] = uRow[col]
                col += pixelStride
            }
        }
        return nv21
    }
}
