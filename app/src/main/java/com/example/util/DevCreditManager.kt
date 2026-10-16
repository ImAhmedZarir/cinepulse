package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object DevCreditManager {
    private const val PREFS_NAME = "cinepulse_dev_credit"
    private const val KEY_SNOOZE_UNTIL = "dev_credit_snooze_until"
    val DEV_NAME: String get() = SecureVault.devName
    val DEV_URL: String get() = SecureVault.devUrl

    fun shouldShowDialog(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snoozeUntil = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        return System.currentTimeMillis() >= snoozeUntil
    }

    fun snoozeFor7Days(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000L
        prefs.edit().putLong(KEY_SNOOZE_UNTIL, System.currentTimeMillis() + sevenDaysMs).apply()
    }

    fun openWebsite(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEV_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
