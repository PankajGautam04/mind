package com.mind.app.capture

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Optional, strictly on-demand AccessibilityService enabling zero-prompt single-frame
 * screenshots on Android 11+ (API 30+) when the user triggers any Mind capture shortcut.
 * `canRetrieveWindowContent` is explicitly false: it never reads window trees or keystrokes.
 */
@AndroidEntryPoint
class MindScreenshotAccessibilityService : AccessibilityService() {

    @Inject lateinit var localMediaStorage: LocalMediaStorage
    @Inject lateinit var captureMemoryUseCase: CaptureMemoryUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (activeInstance === this) {
            activeInstance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun takeSingleScreenshot(
        captureMethod: CaptureMethod,
        retryAttempt: Int = 0,
        onFailureFallback: () -> Unit,
    ) {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val softwareBitmap = screenshot.hardwareBuffer.use { buffer ->
                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                            ?: return@use null
                        try {
                            hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        } finally {
                            hardwareBitmap.recycle()
                        }
                    }
                    if (softwareBitmap == null) {
                        onFailureFallback()
                        return
                    }

                    persistCapturedBitmap(softwareBitmap, captureMethod)
                }

                override fun onFailure(errorCode: Int) {
                    if (errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT && retryAttempt == 0) {
                        mainHandler.postDelayed(
                            { takeSingleScreenshot(captureMethod, retryAttempt = 1, onFailureFallback) },
                            500L,
                        )
                        return
                    }
                    if (errorCode == ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS ||
                        errorCode == ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR ||
                        retryAttempt > 0
                    ) {
                        onFailureFallback()
                    }
                }
            },
        )
    }

    private fun persistCapturedBitmap(bitmap: Bitmap, captureMethod: CaptureMethod) {
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

                withContext(Dispatchers.Main) {
                    val overlayIntent = Intent(
                        this@MindScreenshotAccessibilityService,
                        SaveOverlayActivity::class.java,
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        putExtra(SaveOverlayActivity.EXTRA_MEMORY_ID, memory.id)
                        putExtra(SaveOverlayActivity.EXTRA_MESSAGE, "Saved to Mind")
                        putExtra(SaveOverlayActivity.EXTRA_THUMBNAIL_PATH, thumbPath)
                        putExtra(SaveOverlayActivity.EXTRA_SCREENSHOT_PATH, originalPath)
                    }
                    startActivity(overlayIntent)
                }
            } catch (_: Throwable) {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    companion object {
        @Volatile
        private var activeInstance: MindScreenshotAccessibilityService? = null

        fun isServiceActive(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activeInstance != null

        fun isAccessibilitySettingEnabled(context: Context): Boolean {
            if (isServiceActive()) return true
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0,
            ) == 1
            if (!enabled) return false
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val expected = ComponentName(context, MindScreenshotAccessibilityService::class.java)
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)
            while (splitter.hasNext()) {
                val component = ComponentName.unflattenFromString(splitter.next())
                if (component == expected) return true
            }
            return false
        }

        fun captureSilent(
            captureMethod: CaptureMethod,
            delayMs: Long = 320L,
            onUnavailableOrFailed: () -> Unit = {},
        ): Boolean {
            val service = activeInstance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

            service.mainHandler.postDelayed(
                {
                    val current = activeInstance
                    if (current == null) {
                        onUnavailableOrFailed()
                    } else {
                        current.takeSingleScreenshot(
                            captureMethod = captureMethod,
                            retryAttempt = 0,
                            onFailureFallback = onUnavailableOrFailed,
                        )
                    }
                },
                delayMs,
            )
            return true
        }
    }
}
