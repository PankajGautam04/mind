package com.mind.app

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mind.app.capture.CaptureTriggerActivity
import com.mind.app.capture.MindCaptureWidgetProvider
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Root application class initializing Hilt dependency injection, Hilt-backed WorkManager,
 * and runtime app shortcuts targeting the active packageName (both debug & release).
 */
@HiltAndroidApp
class MindApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerDynamicShortcuts()
    }

    private fun registerDynamicShortcuts() {
        runCatching {
            val shortcuts = listOf(
                ShortcutInfoCompat.Builder(this, "dyn_capture_screen")
                    .setShortLabel(getString(R.string.shortcut_capture_short))
                    .setLongLabel(getString(R.string.shortcut_capture_long))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(
                        Intent(this, CaptureTriggerActivity::class.java).apply {
                            action = "com.mind.app.ACTION_CAPTURE_SCREEN"
                            `package` = packageName
                        },
                    )
                    .setRank(0)
                    .build(),
                ShortcutInfoCompat.Builder(this, "dyn_voice_memory")
                    .setShortLabel(getString(R.string.shortcut_voice_short))
                    .setLongLabel(getString(R.string.shortcut_voice_long))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(
                        Intent(this, MainActivity::class.java).apply {
                            action = MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE
                            `package` = packageName
                        },
                    )
                    .setRank(1)
                    .build(),
                ShortcutInfoCompat.Builder(this, "dyn_quick_note")
                    .setShortLabel(getString(R.string.shortcut_note_short))
                    .setLongLabel(getString(R.string.shortcut_note_long))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(
                        Intent(this, MainActivity::class.java).apply {
                            action = MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE
                            `package` = packageName
                        },
                    )
                    .setRank(2)
                    .build(),
                ShortcutInfoCompat.Builder(this, "dyn_search")
                    .setShortLabel(getString(R.string.shortcut_search_short))
                    .setLongLabel(getString(R.string.shortcut_search_long))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(
                        Intent(this, MainActivity::class.java).apply {
                            action = MindCaptureWidgetProvider.ACTION_OPEN_SEARCH
                            `package` = packageName
                        },
                    )
                    .setRank(3)
                    .build(),
            )
            ShortcutManagerCompat.setDynamicShortcuts(this, shortcuts)
        }
    }
}
