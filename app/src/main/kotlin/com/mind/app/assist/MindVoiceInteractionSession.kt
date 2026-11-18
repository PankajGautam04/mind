package com.mind.app.assist

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mind.app.MainActivity
import com.mind.app.R
import com.mind.app.capture.CaptureTriggerActivity
import com.mind.app.capture.MindCaptureWidgetProvider
import com.mind.app.capture.MindScreenshotAccessibilityService
import com.mind.app.capture.SaveOverlayActivity
import com.mind.app.core.data.storage.LocalMediaStorage
import com.mind.app.core.designsystem.component.MindCaptureSnapshotCache
import com.mind.app.core.designsystem.theme.MindColors
import com.mind.app.core.designsystem.theme.mindDarkColors
import com.mind.app.core.designsystem.theme.mindLightColors
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URI
import java.util.UUID

/**
 * Assistant interaction session invoked when Mind is the active Default Digital Assistant
 * (e.g., via Samsung Side Button long-press or home corner assist gesture).
 *
 * Respects the user's configured [AssistantAction]: showing either a sleek One UI picker overlay
 * with instant pre-captured screen preview, or immediately executing their chosen Mind action.
 */
class MindVoiceInteractionSession(
    private val context: Context,
    private val localMediaStorage: LocalMediaStorage,
    private val captureMemoryUseCase: CaptureMemoryUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val serviceScope: CoroutineScope,
) : VoiceInteractionSession(context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentPrefs: UserPreferences = UserPreferences()
    private var configuredAction: AssistantAction = AssistantAction.ASK_EVERY_TIME

    private var latestScreenshot: Bitmap? = null
    private var screenshotCallbackSeen: Boolean = false
    private var immediateActionDispatched: Boolean = false
    private var detectedWebUrl: String? = null
    private var detectedSourcePackage: String? = null
    private var isSessionAlive: Boolean = true
    private var pendingFallbackRunnable: Runnable? = null

    private var sheetContainerView: LinearLayout? = null
    private var statusBadgeView: TextView? = null
    private var previewImageView: ImageView? = null
    private var screenSubtitleView: TextView? = null
    private var linkRowView: LinearLayout? = null
    private var linkSubtitleView: TextView? = null

    override fun onPrepareShow(args: Bundle?, showFlags: Int) {
        super.onPrepareShow(args, showFlags)
        cancelPendingFallback()
        recycleHeldScreenshot()

        isSessionAlive = true
        screenshotCallbackSeen = false
        immediateActionDispatched = false
        detectedWebUrl = null
        detectedSourcePackage = null

        currentPrefs = runCatching {
            runBlocking { preferencesRepository.observePreferences().first() }
        }.getOrDefault(UserPreferences())
        configuredAction = currentPrefs.assistantAction

        setUiEnabled(configuredAction == AssistantAction.ASK_EVERY_TIME)
    }

    override fun onCreateContentView(): View {
        val colors = resolveColors(currentPrefs)
        val supportsBehindBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        val root = FrameLayout(context).apply {
            setBackgroundColor(if (supportsBehindBlur) 0x26000000 else 0x52000000)
            isClickable = true
            setOnClickListener { finish() }
        }

        val sheetBgColor = if (supportsBehindBlur) {
            colors.glassSheetFill.toArgb()
        } else {
            colors.surfaceElevated.copy(alpha = 0.96f).toArgb()
        }
        val sheetStrokeColor = colors.rimHighlight.toArgb()

        val sheet = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(28).toFloat()
                setColor(sheetBgColor)
                setStroke(dp(1), sheetStrokeColor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                outlineSpotShadowColor = colors.shadowInk.copy(alpha = 0.28f).toArgb()
            }
            elevation = dp(12).toFloat()
            setPadding(dp(18), dp(10), dp(18), dp(16))
        }.also { sheetContainerView = it }

        val handlePill = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(10)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(2).toFloat()
                setColor(colors.hairline.copy(alpha = 0.35f).toArgb())
            }
        }
        sheet.addView(handlePill)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = sheet.layoutParams as? FrameLayout.LayoutParams
            if (lp != null) {
                lp.setMargins(dp(12), dp(12), dp(12), dp(14) + navBottom)
                sheet.layoutParams = lp
            }
            insets
        }

        // Header Row
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(10))
        }

        val headerTextCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            text = context.getString(R.string.assistant_service_label)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(colors.textPrimary.toArgb())
        }

        val statusView = TextView(context).apply {
            text = "Preparing current screen..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(colors.accent.toArgb())
            setPadding(0, dp(2), 0, 0)
        }.also { statusBadgeView = it }

        headerTextCol.addView(titleView)
        headerTextCol.addView(statusView)

        val closePill = TextView(context).apply {
            text = "✕"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(colors.textSecondary.toArgb())
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colors.glassPillFill.toArgb())
                setStroke(dp(1), colors.hairline.toArgb())
            }
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
            setOnClickListener { finish() }
        }

        headerRow.addView(headerTextCol)
        headerRow.addView(closePill)
        sheet.addView(headerRow)

        // Primary Row: Save Screen (uses OS onHandleScreenshot when ready, else falls back cleanly)
        val saveScreenCard = buildActionRow(
            colors = colors,
            iconRes = R.drawable.ic_mind_tile,
            title = "Save Screen",
            subtitle = "Capture visible screen & organize with AI",
            isHighlighted = true,
            includeThumbnailSlot = true,
            onClick = { onSaveScreenSelected() },
        ) { subtitleRef, thumbRef ->
            screenSubtitleView = subtitleRef
            previewImageView = thumbRef
        }
        sheet.addView(saveScreenCard, actionRowLayoutParams())

        // Conditional Webpage Link Row (shown automatically when foreground app shares a webUri)
        val linkCard = buildActionRow(
            colors = colors,
            iconRes = R.drawable.ic_widget_note,
            title = "Save Page Link",
            subtitle = "",
            isHighlighted = false,
            includeThumbnailSlot = false,
            onClick = {
                val url = detectedWebUrl
                hide()
                launchMainAction(
                    action = MindCaptureWidgetProvider.ACTION_OPEN_LINK_CAPTURE,
                    prefillUrl = url,
                )
                finish()
            },
        ) { subtitleRef, _ ->
            linkSubtitleView = subtitleRef
        }.apply {
            visibility = View.GONE
        }.also { linkRowView = it }
        sheet.addView(linkCard, actionRowLayoutParams())

        // Quick Note Row
        sheet.addView(
            buildActionRow(
                colors = colors,
                iconRes = R.drawable.ic_widget_note,
                title = "Quick Note or Quote",
                subtitle = "Write a thought, snippet, or paste a link",
                isHighlighted = false,
                includeThumbnailSlot = false,
                onClick = {
                    hide()
                    launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE)
                    finish()
                },
            ),
            actionRowLayoutParams(),
        )

        // Voice Memory Row
        sheet.addView(
            buildActionRow(
                colors = colors,
                iconRes = R.drawable.ic_widget_voice,
                title = "Record Voice Memory",
                subtitle = "Save a spoken note with AI summary",
                isHighlighted = false,
                includeThumbnailSlot = false,
                onClick = {
                    hide()
                    launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE)
                    finish()
                },
            ),
            actionRowLayoutParams(),
        )

        // Ask Mind Row
        sheet.addView(
            buildActionRow(
                colors = colors,
                iconRes = R.drawable.ic_widget_search,
                title = "Ask Mind (AI Search)",
                subtitle = "Search screenshots, notes, and quotes naturally",
                isHighlighted = false,
                includeThumbnailSlot = false,
                onClick = {
                    hide()
                    launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_SEARCH)
                    finish()
                },
            ),
            actionRowLayoutParams(),
        )

        // Footer row: Change default Power/Side Button action
        val footerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), 0)
        }

        val defaultActionLink = TextView(context).apply {
            text = "Set default power-button action  ›"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(colors.accent.toArgb())
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener {
                hide()
                launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_SETTINGS)
                finish()
            }
        }
        footerRow.addView(defaultActionLink)
        sheet.addView(footerRow)

        val sheetLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ).apply {
            setMargins(dp(12), dp(12), dp(12), dp(18))
        }
        root.addView(sheet, sheetLp)
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        if (configuredAction == AssistantAction.ASK_EVERY_TIME) {
            applyGlassWindowAttributes()
            animateSheetEntrance()
            if ((showFlags and SHOW_WITH_SCREENSHOT) == 0 && !screenshotCallbackSeen) {
                updatePreviewStatusUi()
            }
            return
        }

        if (immediateActionDispatched) return

        when (configuredAction) {
            AssistantAction.CAPTURE_SCREEN -> {
                val readyBitmap = takeHeldScreenshot()
                if (readyBitmap != null) {
                    immediateActionDispatched = true
                    persistCapturedBitmapAndNotify(readyBitmap)
                } else if (screenshotCallbackSeen) {
                    immediateActionDispatched = true
                    triggerFallbackScreenCaptureAndFinish()
                } else {
                    // Always allow asynchronous onHandleScreenshot a brief window before falling back
                    // so silent OS assistant screenshots work reliably on One UI & Pixel even when showFlags omit SHOW_WITH_SCREENSHOT.
                    scheduleImmediateFallbackTimeout()
                }
            }
            AssistantAction.VOICE_MEMORY -> {
                immediateActionDispatched = true
                launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE)
                finish()
            }
            AssistantAction.QUICK_NOTE -> {
                immediateActionDispatched = true
                launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE)
                finish()
            }
            AssistantAction.OPEN_SEARCH -> {
                immediateActionDispatched = true
                launchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_SEARCH)
                finish()
            }
            AssistantAction.ASK_EVERY_TIME -> Unit
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        cancelPendingFallback()
        screenshotCallbackSeen = true

        val copiedBitmap = if (screenshot != null) {
            runCatching { screenshot.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
        } else {
            null
        }
        replaceHeldScreenshot(copiedBitmap)

        if (configuredAction == AssistantAction.CAPTURE_SCREEN && !immediateActionDispatched) {
            immediateActionDispatched = true
            val bitmapToSave = takeHeldScreenshot()
            if (bitmapToSave != null) {
                persistCapturedBitmapAndNotify(bitmapToSave)
            } else {
                triggerFallbackScreenCaptureAndFinish()
            }
            return
        }

        if (configuredAction == AssistantAction.ASK_EVERY_TIME) {
            updatePreviewStatusUi()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?,
    ) {
        @Suppress("DEPRECATION")
        super.onHandleAssist(data, structure, content)
        handleAssistPayload(structure, content)
    }

    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        handleAssistPayload(state.assistStructure, state.assistContent)
    }

    override fun onHide() {
        cancelPendingFallback()
        super.onHide()
    }

    override fun onDestroy() {
        isSessionAlive = false
        cancelPendingFallback()
        recycleHeldScreenshot()
        statusBadgeView = null
        previewImageView = null
        screenSubtitleView = null
        linkRowView = null
        linkSubtitleView = null
        super.onDestroy()
    }

    private fun handleAssistPayload(structure: AssistStructure?, content: AssistContent?) {
        val pkg = structure?.activityComponent?.packageName?.takeIf { it.isNotBlank() }
        if (pkg != null) {
            detectedSourcePackage = pkg
        }
        val url = content?.webUri?.toString()?.takeIf { it.isNotBlank() }
        if (url != null) {
            detectedWebUrl = url
            updateDetectedLinkRow(url)
        }
    }

    private fun onSaveScreenSelected() {
        hide()
        val bitmapToSave = takeHeldScreenshot()
        if (bitmapToSave != null) {
            persistCapturedBitmapAndNotify(bitmapToSave)
        } else {
            triggerFallbackScreenCaptureAndFinish()
        }
    }

    private fun scheduleImmediateFallbackTimeout() {
        cancelPendingFallback()
        val runnable = Runnable {
            pendingFallbackRunnable = null
            if (!immediateActionDispatched && isSessionAlive) {
                immediateActionDispatched = true
                val bitmapToSave = takeHeldScreenshot()
                if (bitmapToSave != null) {
                    persistCapturedBitmapAndNotify(bitmapToSave)
                } else {
                    triggerFallbackScreenCaptureAndFinish()
                }
            }
        }
        pendingFallbackRunnable = runnable
        mainHandler.postDelayed(runnable, OS_SCREENSHOT_TIMEOUT_MS)
    }

    private fun cancelPendingFallback() {
        pendingFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingFallbackRunnable = null
    }

    private fun persistCapturedBitmapAndNotify(bitmap: Bitmap) {
        val capturedPkg = detectedSourcePackage
        val capturedUrl = detectedWebUrl
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
                    captureMethod = CaptureMethod.ASSISTANT,
                    sourcePackageName = capturedPkg,
                    sourceAppName = capturedPkg?.let(::resolveAppDisplayName),
                    originalUrl = capturedUrl,
                )

                withContext(Dispatchers.Main) {
                    val overlayIntent = Intent(context, SaveOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        putExtra(SaveOverlayActivity.EXTRA_MEMORY_ID, memory.id)
                        putExtra(SaveOverlayActivity.EXTRA_MESSAGE, "Saved to Mind")
                        putExtra(SaveOverlayActivity.EXTRA_THUMBNAIL_PATH, thumbPath)
                        putExtra(SaveOverlayActivity.EXTRA_SCREENSHOT_PATH, originalPath)
                    }
                    launchActivitySafely(overlayIntent)
                    if (isSessionAlive) {
                        finish()
                    }
                }
            } catch (_: Throwable) {
                bitmap.recycle()
                withContext(Dispatchers.Main) {
                    if (isSessionAlive) {
                        triggerFallbackScreenCaptureAndFinish()
                    }
                }
            }
        }
    }

    private fun triggerFallbackScreenCaptureAndFinish() {
        val appContext = context.applicationContext
        val dispatchedSilent = MindScreenshotAccessibilityService.isServiceActive() &&
            MindScreenshotAccessibilityService.captureSilent(
                captureMethod = CaptureMethod.ASSISTANT,
                delayMs = 240L,
                onUnavailableOrFailed = {
                    appContext.startActivity(
                        CaptureTriggerActivity.createIntent(
                            context = appContext,
                            method = CaptureMethod.ASSISTANT,
                            forceMediaProjection = false,
                            showSetupOnly = true,
                        ),
                    )
                },
            )
        if (!dispatchedSilent) {
            launchActivitySafely(
                CaptureTriggerActivity.createIntent(
                    context = context,
                    method = CaptureMethod.ASSISTANT,
                    forceMediaProjection = false,
                ),
            )
        }
        if (isSessionAlive) {
            finish()
        }
    }

    private fun launchMainAction(action: String, prefillUrl: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!prefillUrl.isNullOrBlank()) {
                putExtra(MindCaptureWidgetProvider.EXTRA_PREFILL_URL, prefillUrl)
            }
        }
        launchActivitySafely(intent)
    }

    private fun launchActivitySafely(intent: Intent) {
        val launchedViaSession = isSessionAlive && runCatching {
            @Suppress("DEPRECATION")
            startAssistantActivity(intent)
        }.isSuccess
        if (!launchedViaSession) {
            runCatching {
                context.applicationContext.startActivity(
                    intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                )
            }
        }
    }

    private fun updatePreviewStatusUi() {
        val bmp = latestScreenshot
        if (bmp != null && !bmp.isRecycled) {
            statusBadgeView?.text = "Screen captured • Tap Save Screen for instant save"
            screenSubtitleView?.text = "Silent save with pre-captured screen"
            previewImageView?.apply {
                setImageBitmap(bmp)
                visibility = View.VISIBLE
            }
        } else {
            statusBadgeView?.text = "Side Button Assistant • Ready"
            screenSubtitleView?.text = "Capture visible screen & organize with AI"
            previewImageView?.apply {
                setImageDrawable(null)
                visibility = View.GONE
            }
        }
    }

    private fun updateDetectedLinkRow(url: String) {
        val host = runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()
        linkSubtitleView?.text = host?.takeIf { it.isNotBlank() } ?: url
        linkRowView?.visibility = View.VISIBLE
    }

    private fun resolveAppDisplayName(packageName: String): String? {
        return runCatching {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrNull()
    }

    private fun resolveColors(prefs: UserPreferences): MindColors {
        val dark = when (prefs.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.FOLLOW_SYSTEM -> {
                val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return if (dark) mindDarkColors(prefs.accentPalette) else mindLightColors(prefs.accentPalette)
    }

    private fun replaceHeldScreenshot(newBitmap: Bitmap?) {
        val previous = latestScreenshot
        latestScreenshot = newBitmap
        if (previous != null && previous !== newBitmap && !previous.isRecycled) {
            previous.recycle()
        }
    }

    private fun takeHeldScreenshot(): Bitmap? {
        val held = latestScreenshot ?: return null
        latestScreenshot = null
        return if (!held.isRecycled) held else null
    }

    private fun recycleHeldScreenshot() {
        latestScreenshot?.let { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }
        latestScreenshot = null
    }

    private fun applyGlassWindowAttributes() {
        if (configuredAction != AssistantAction.ASK_EVERY_TIME) return
        val win = runCatching { window?.window }.getOrNull() ?: return
        val params = win.attributes
        params.dimAmount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.22f else 0.34f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            params.blurBehindRadius = dp(24)
        }
        win.attributes = params
    }

    private fun animateSheetEntrance() {
        val sheet = sheetContainerView ?: return
        sheet.animate().cancel()
        sheet.translationY = dp(22).toFloat()
        sheet.alpha = 0f
        sheet.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(260L)
            .setInterpolator(PathInterpolator(0.22f, 1f, 0.36f, 1f))
            .start()
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun actionRowLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(8)
        }
    }

    private fun buildActionRow(
        colors: MindColors,
        iconRes: Int,
        title: String,
        subtitle: String,
        isHighlighted: Boolean,
        includeThumbnailSlot: Boolean,
        onClick: () -> Unit,
        onBindRefs: (subtitleView: TextView, previewView: ImageView?) -> Unit = { _, _ -> },
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                val baseColor = if (isHighlighted) colors.accentSoft else colors.glassCardFill
                setColor(baseColor.copy(alpha = if (isHighlighted) 0.85f else 0.76f).toArgb())
                setStroke(
                    dp(1),
                    colors.hairline.toArgb(),
                )
            }
            setOnClickListener { onClick() }
        }

        val iconHolder = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isHighlighted) colors.accent.toArgb() else colors.identityContainers[0].toArgb())
            }
        }
        val iconView = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(if (isHighlighted) colors.onAccent.toArgb() else colors.onIdentityContainers[0].toArgb())
            layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        }
        iconHolder.addView(iconView)
        row.addView(iconHolder)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
                marginEnd = dp(8)
            }
        }
        val titleView = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(colors.textPrimary.toArgb())
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val subtitleView = TextView(context).apply {
            text = subtitle
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(colors.textSecondary.toArgb())
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(2), 0, 0)
        }
        textCol.addView(titleView)
        textCol.addView(subtitleView)
        row.addView(textCol)

        var previewView: ImageView? = null
        if (includeThumbnailSlot) {
            previewView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(48))
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(8).toFloat()
                    setColor(colors.surface.toArgb())
                }
                visibility = View.GONE
            }
            row.addView(previewView)
        }

        onBindRefs(subtitleView, previewView)
        return row
    }

    private companion object {
        const val OS_SCREENSHOT_TIMEOUT_MS = 450L
    }
}
