package com.gow.smaitrobot

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SMAITRobot"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    // UI elements
    private lateinit var edgeIpInput: EditText
    private lateinit var edgePortInput: EditText
    private lateinit var connectButton: Button
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var cameraPreview: TextureView

    // WebSocket
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val isStreaming = AtomicBoolean(false)

    // Camera
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private lateinit var cameraHandler: Handler
    private lateinit var cameraThread: HandlerThread

    // Audio
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null

    // TTS
    private var tts: TextToSpeech? = null

    // Stats
    private var frameCount = 0
    private var audioChunkCount = 0
    private var lastStatsTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        initTTS()
        checkPermissions()
        startCameraThread()
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                Log.i(TAG, "TTS initialized")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
    }

    private fun initUI() {
        edgeIpInput = findViewById(R.id.edgeIpInput)
        edgePortInput = findViewById(R.id.edgePortInput)
        connectButton = findViewById(R.id.connectButton)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)
        statsText = findViewById(R.id.statsText)
        cameraPreview = findViewById(R.id.cameraPreview)

        connectButton.setOnClickListener {
            if (isStreaming.get()) {
                stopStreaming()
            } else {
                startStreaming()
            }
        }

        cameraPreview.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                if (hasPermissions()) openCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && hasPermissions()) {
            if (cameraPreview.isAvailable) openCamera()
        }
    }

    private fun startCameraThread() {
        cameraThread = HandlerThread("CameraThread").also { it.start() }
        cameraHandler = Handler(cameraThread.looper)
    }

    private fun openCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                image?.let {
                    sendVideoFrame(it)
                    it.close()
                }
            }, cameraHandler)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    Log.e(TAG, "Camera error: $error")
                }
            }, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSession() {
        try {
            val texture = cameraPreview.surfaceTexture
            texture?.setDefaultBufferSize(640, 480)
            val previewSurface = Surface(texture)

            val surfaces = listOf(previewSurface, imageReader!!.surface)

            cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    requestBuilder?.addTarget(previewSurface)
                    requestBuilder?.addTarget(imageReader!!.surface)
                    requestBuilder?.let {
                        session.setRepeatingRequest(it.build(), null, cameraHandler)
                    }
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Capture session configuration failed")
                }
            }, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session", e)
        }
    }

    private fun startStreaming() {
        val ip = edgeIpInput.text.toString()
        val port = edgePortInput.text.toString()
        val url = "ws://$ip:$port"

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    isStreaming.set(true)
                    connectButton.text = "Stop Streaming"
                    statusIndicator.setBackgroundColor(0xFF44FF44.toInt())
                    statusText.text = "Connected to $ip:$port"
                }
                startAudioCapture()
                Log.i(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    isStreaming.set(false)
                    connectButton.text = "Start Streaming"
                    statusIndicator.setBackgroundColor(0xFFFF4444.toInt())
                    statusText.text = "Connection failed: ${t.message}"
                }
                Log.e(TAG, "WebSocket failed", t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    isStreaming.set(false)
                    connectButton.text = "Start Streaming"
                    statusIndicator.setBackgroundColor(0xFFFF4444.toInt())
                    statusText.text = "Disconnected"
                }
            }
        })
    }

    private fun stopStreaming() {
        isStreaming.set(false)
        webSocket?.close(1000, "User stopped")
        webSocket = null
        stopAudioCapture()
        runOnUiThread {
            connectButton.text = "Start Streaming"
            statusIndicator.setBackgroundColor(0xFFFF4444.toInt())
            statusText.text = "Disconnected"
        }
    }

    private fun startAudioCapture() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        audioRecord?.startRecording()

        audioThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (isStreaming.get()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    sendAudioChunk(buffer.copyOf(read))
                }
            }
        }.also { it.start() }
    }

    private fun stopAudioCapture() {
        audioThread?.interrupt()
        audioThread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun sendVideoFrame(image: Image) {
        if (!isStreaming.get()) return

        try {
            // Convert YUV to JPEG
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
            val jpegBytes = out.toByteArray()

            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            val json = JSONObject()
            json.put("type", "video")
            json.put("timestamp", System.currentTimeMillis())
            json.put("data", base64)
            webSocket?.send(json.toString())
            frameCount++
            updateStats()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send video frame", e)
        }
    }

    private fun sendAudioChunk(chunk: ByteArray) {
        if (!isStreaming.get()) return

        try {
            val base64 = Base64.encodeToString(chunk, Base64.NO_WRAP)
            val json = JSONObject()
            json.put("type", "audio")
            json.put("timestamp", System.currentTimeMillis())
            json.put("data", base64)
            webSocket?.send(json.toString())
            audioChunkCount++
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send audio chunk", e)
        }
    }

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.getString("type")) {
                "tts" -> {
                    val ttsText = json.getString("text")
                    Log.i(TAG, "TTS received: $ttsText")
                    speakText(ttsText)
                }
                "status" -> {
                    val listening = json.optBoolean("listening", false)
                    Log.i(TAG, "Status update - listening: $listening")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse server message", e)
        }
    }

    private fun updateStats() {
        val now = System.currentTimeMillis()
        if (now - lastStatsTime >= 1000) {
            val fps = frameCount
            val aps = audioChunkCount
            frameCount = 0
            audioChunkCount = 0
            lastStatsTime = now

            runOnUiThread {
                statsText.text = "Video: $fps fps | Audio: $aps chunks/s"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        stopStreaming()
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        cameraThread.quitSafely()
    }
}