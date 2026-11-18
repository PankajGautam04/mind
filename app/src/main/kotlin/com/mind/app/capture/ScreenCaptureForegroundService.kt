package com.mind.app.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import com.mind.app.R
import com.mind.app.core.data.storage.LocalMediaStorage
import com.mind.app.core.designsystem.component.MindCaptureSnapshotCache
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ContentType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Foreground service (`mediaProjection`) that captures a single screen frame after explicit
 * user consent, persists it locally immediately, enqueues Gemini analysis, and displays
 * Mind's signature save animation overlay.
 */
@AndroidEntryPoint
class ScreenCaptureForegroundService : Service() {

    @Inject lateinit var localMediaStorage: LocalMediaStorage
    @Inject lateinit var captureMemoryUseCase: CaptureMemoryUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var handlerThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    @Volatile private var hasCaptured = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = intent?.let {
            IntentCompat.getParcelableExtra(it, EXTRA_RESULT_DATA, Intent::class.java)
        }
        val captureMethodName = intent?.getStringExtra(EXTRA_CAPTURE_METHOD)
        val captureMethod = runCatching {
            CaptureMethod.valueOf(captureMethodName ?: CaptureMethod.QUICK_SETTINGS.name)
        }.getOrDefault(CaptureMethod.QUICK_SETTINGS)

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        hasCaptured = false
        val thread = HandlerThread("MindCaptureThread").apply { start() }
        handlerThread = thread
        val handler = Handler(thread.looper)
        captureHandler = handler

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        mediaProjection = projection

        projection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    cleanupCaptureResources()
                }
            },
            handler,
        )

        // Wait briefly so the system consent dialog and translucent trigger activity vanish cleanly.
        handler.postDelayed(
            { startSingleFrameCapture(projection, handler, captureMethod) },
            280L,
        )

        return START_NOT_STICKY
    }

    private fun startSingleFrameCapture(
        projection: MediaProjection,
        handler: Handler,
        captureMethod: CaptureMethod,
    ) {
        val (width, height, densityDpi) = resolveScreenMetrics()
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener(
            { ir ->
                if (hasCaptured) return@setOnImageAvailableListener
                val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                hasCaptured = true

                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val fullBitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888,
                    )
                    fullBitmap.copyPixelsFromBuffer(buffer)
                    val croppedBitmap = if (rowPadding == 0) {
                        fullBitmap
                    } else {
                        Bitmap.createBitmap(fullBitmap, 0, 0, width, height).also {
                            fullBitmap.recycle()
                        }
                    }

                    persistAndNotify(croppedBitmap, captureMethod)
                } finally {
                    image.close()
                    cleanupCaptureResources()
                }
            },
            handler,
        )

        virtualDisplay = projection.createVirtualDisplay(
            "MindScreenCapture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        )
    }

    private fun persistAndNotify(bitmap: Bitmap, captureMethod: CaptureMethod) {
        MindCaptureSnapshotCache.put(bitmap)
        serviceScope.launch {
            try {
                val memoryKey = "cap_${UUID.randomUUID()}"
                val (originalPath, thumbPath) = localMediaStorage.saveBitmapAndThumbnail(bitmap, memoryKey)
                bitmap.recycle()

                val memory = captureMemoryUseCase.saveImageMemory(
                    originalFilePath = originalPath,
                    thumbnailPath = thumbPath,
                    contentType = ContentType.SCREENSHOT,
                    captureMethod = captureMethod,
                )

                Handler(Looper.getMainLooper()).post {
                    val overlayIntent = Intent(this@ScreenCaptureForegroundService, SaveOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        putExtra(SaveOverlayActivity.EXTRA_MEMORY_ID, memory.id)
                        putExtra(SaveOverlayActivity.EXTRA_MESSAGE, "Saved to Mind")
                        putExtra(SaveOverlayActivity.EXTRA_THUMBNAIL_PATH, thumbPath)
                        putExtra(SaveOverlayActivity.EXTRA_SCREENSHOT_PATH, originalPath)
                    }
                    startActivity(overlayIntent)
                }
            } finally {
                stopSelf()
            }
        }
    }

    private fun resolveScreenMetrics(): Triple<Int, Int, Int> {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val densityDpi = resources.displayMetrics.densityDpi
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Triple(bounds.width().coerceAtLeast(1), bounds.height().coerceAtLeast(1), densityDpi)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Triple(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1), densityDpi)
        }
    }

    private fun cleanupCaptureResources() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { handlerThread?.quitSafely() }
        handlerThread = null
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mind Screen Capture",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active while capturing the current screen to Mind"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mind_tile)
            .setContentTitle("Saving screen to Mind")
            .setContentText("Capturing current screen locally...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        cleanupCaptureResources()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_CAPTURE_METHOD = "extra_capture_method"
        private const val CHANNEL_ID = "mind_capture_channel"
        private const val NOTIFICATION_ID = 4011
    }
}
