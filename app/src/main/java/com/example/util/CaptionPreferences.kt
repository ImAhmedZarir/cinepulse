package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists user preferences for subtitle/caption visual style:
 * - Text opacity / transparency (0.2f - 1.0f)
 * - Background opacity / transparency (0.0f - 1.0f)
 */
object CaptionPreferences {
    private const val PREFS_NAME = "cinepulse_caption_prefs"
    private const val KEY_TEXT_OPACITY = "caption_text_opacity"
    private const val KEY_BG_OPACITY = "caption_bg_opacity"

    const val DEFAULT_TEXT_OPACITY = 1.0f
    const val DEFAULT_BG_OPACITY = 0.75f

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTextOpacity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_TEXT_OPACITY, DEFAULT_TEXT_OPACITY)
            .coerceIn(0.2f, 1.0f)
    }

    fun setTextOpacity(context: Context, opacity: Float) {
        val clamped = opacity.coerceIn(0.2f, 1.0f)
        getPrefs(context).edit().putFloat(KEY_TEXT_OPACITY, clamped).apply()
    }

    fun getBackgroundOpacity(context: Context): Float {
        return getPrefs(context).getFloat(KEY_BG_OPACITY, DEFAULT_BG_OPACITY)
            .coerceIn(0.0f, 1.0f)
    }

    fun setBackgroundOpacity(context: Context, opacity: Float) {
        val clamped = opacity.coerceIn(0.0f, 1.0f)
        getPrefs(context).edit().putFloat(KEY_BG_OPACITY, clamped).apply()
    }
}
