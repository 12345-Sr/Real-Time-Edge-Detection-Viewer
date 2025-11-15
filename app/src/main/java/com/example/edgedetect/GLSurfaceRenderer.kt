
package com.example.edgedetect

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLSurfaceView.Renderer implementation. The `updateFrame` method may be called from any thread;
 * the renderer safely copies the supplied RGBA bytes into a buffer and uploads them in onDrawFrame.
 */
class GLSurfaceRenderer(private val ctx: Context) : GLSurfaceView.Renderer {
    private var textureId = 0
    private var program = 0
    private var vertexBuffer: FloatBuffer
    private var texBuffer: FloatBuffer

    @Volatile private var pending: ByteBuffer? = null
    private var frameW = 0
    private var frameH = 0

    init {
        val v = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val t = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
        vertexBuffer = ByteBuffer.allocateDirect(v.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(v); position(0) }
        texBuffer = ByteBuffer.allocateDirect(t.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(t); position(0) }
    }

    fun updateFrame(rgba: ByteArray, width: Int, height: Int) {
        // copy into ByteBuffer and mark pending; onDrawFrame will upload
        val bb = ByteBuffer.allocateDirect(rgba.size).order(ByteOrder.nativeOrder())
        bb.put(rgba).position(0)
        pending = bb
        frameW = width; frameH = height
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        textureId = createTexture()
        GLES20.glUseProgram(program)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val pendingBuf = pending
        if (pendingBuf != null && frameW > 0 && frameH > 0) {
            // upload RGBA to texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, frameW, frameH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pendingBuf)
            pending = null
        }

        GLES20.glUseProgram(program)
        val posLoc = GLES20.glGetAttribLocation(program, "aPosition")
        val texLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        val texUniform = GLES20.glGetUniformLocation(program, "uTexture")

        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(texUniform, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(texLoc)
    }

    private fun createTexture(): Int {
        val t = IntArray(1)
        GLES20.glGenTextures(1, t, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return t[0]
    }

    private fun loadShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vs: String, fs: String): Int {
        val v = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        return p
    }

    companion object {
        private const val VERTEX_SHADER = """                attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """                precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
