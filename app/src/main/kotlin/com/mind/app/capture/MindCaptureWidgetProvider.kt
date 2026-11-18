package com.mind.app.capture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mind.app.MainActivity
import com.mind.app.R

/**
 * Clean, spacious capsule Home-screen widget with a Search bar and circular Voice, Note, and Add orbs.
 */
class MindCaptureWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_mind_capture)

            views.setOnClickPendingIntent(
                R.id.widget_btn_search,
                mainActionPendingIntent(context, 101, ACTION_OPEN_SEARCH),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_voice,
                mainActionPendingIntent(context, 102, ACTION_OPEN_VOICE_CAPTURE),
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_note,
                mainActionPendingIntent(context, 103, ACTION_OPEN_NOTE_CAPTURE),
            )

            val anythingIntent = CaptureTriggerActivity.createCaptureAnythingIntent(context)
            views.setOnClickPendingIntent(
                R.id.widget_btn_anything,
                PendingIntent.getActivity(
                    context,
                    105,
                    anythingIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun mainActionPendingIntent(context: Context, reqCode: Int, action: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_OPEN_VOICE_CAPTURE = "com.mind.app.ACTION_OPEN_VOICE_CAPTURE"
        const val ACTION_OPEN_NOTE_CAPTURE = "com.mind.app.ACTION_OPEN_NOTE_CAPTURE"
        const val ACTION_OPEN_LINK_CAPTURE = "com.mind.app.ACTION_OPEN_LINK_CAPTURE"
        const val ACTION_OPEN_SEARCH = "com.mind.app.ACTION_OPEN_SEARCH"
        const val ACTION_OPEN_SETTINGS = "com.mind.app.ACTION_OPEN_SETTINGS"
        const val ACTION_OPEN_MEMORY = "com.mind.app.ACTION_OPEN_MEMORY"
        const val EXTRA_PREFILL_URL = "extra_prefill_url"
        const val EXTRA_MEMORY_ID = "extra_memory_id"
    }
}
