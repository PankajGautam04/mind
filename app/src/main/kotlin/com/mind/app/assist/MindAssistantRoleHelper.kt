package com.mind.app.assist

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Checks and requests Android's `ROLE_ASSISTANT` (Default digital assistant app),
 * which lets Samsung Side Button long-press or home assistant gestures invoke Mind directly.
 */
object MindAssistantRoleHelper {

    fun isMindDefaultAssistant(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null) {
                return runCatching {
                    roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
                }.getOrDefault(false)
            }
        }
        val secureAssistant = runCatching {
            Settings.Secure.getString(context.contentResolver, "assistant")
        }.getOrNull()
        return secureAssistant?.startsWith(context.packageName) == true
    }

    fun buildRequestAssistantRoleIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
            }
        }
        return Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
    }

    fun buildDefaultAssistantSettingsIntent(): Intent =
        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
}
