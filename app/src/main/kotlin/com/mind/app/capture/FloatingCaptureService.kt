package com.mind.app.capture

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.mind.app.R
import com.mind.app.core.model.CaptureMethod
import kotlin.math.abs

/**
 * Optional draggable overlay button for users who opt into floating capture in Settings.
 */
class FloatingCaptureService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val density = resources.displayMetrics.density
        val sizePx = (52 * density).toInt()

        val orbBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(232, 17, 19, 26))
            setStroke((1.5f * density).toInt().coerceAtLeast(1), Color.argb(145, 129, 140, 248))
        }

        val buttonView = ImageView(this).apply {
            setImageResource(R.drawable.ic_mind_tile)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            background = orbBackground
            val pad = (13 * density).toInt()
            setPadding(pad, pad, pad, pad)
            contentDescription = "Save screen to Mind"
            elevation = 14f * density
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 32
            y = 320
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        buttonView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    wm.updateViewLayout(buttonView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.rawX - initialTouchX)
                    val dy = abs(event.rawY - initialTouchY)
                    if (dx < 12f && dy < 12f) {
                        if (MindScreenshotAccessibilityService.isServiceActive()) {
                            buttonView.visibility = View.INVISIBLE
                            val dispatched = MindScreenshotAccessibilityService.captureSilent(
                                captureMethod = CaptureMethod.FLOATING_BUTTON,
                                delayMs = 140L,
                                onUnavailableOrFailed = {
                                    buttonView.post { buttonView.visibility = View.VISIBLE }
                                    startActivity(CaptureTriggerActivity.createIntent(this, CaptureMethod.FLOATING_BUTTON))
                                },
                            )
                            buttonView.postDelayed({ buttonView.visibility = View.VISIBLE }, 600L)
                            if (!dispatched) {
                                startActivity(CaptureTriggerActivity.createIntent(this, CaptureMethod.FLOATING_BUTTON))
                            }
                        } else {
                            startActivity(CaptureTriggerActivity.createIntent(this, CaptureMethod.FLOATING_BUTTON))
                        }
                    }
                    true
                }
                else -> false
            }
        }

        wm.addView(buttonView, params)
        floatingView = buttonView
    }

    override fun onDestroy() {
        floatingView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        floatingView = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, FloatingCaptureService::class.java))
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingCaptureService::class.java))
        }
    }
}
