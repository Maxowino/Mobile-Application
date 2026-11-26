package com.example.interviewassistant

import android.content.Context

/** Settings page values, kept on the phone (SharedPreferences). */
object AppPrefs {
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun readAloud(c: Context): Boolean = prefs(c).getBoolean("read_aloud", true)
    fun setReadAloud(c: Context, value: Boolean) = prefs(c).edit().putBoolean("read_aloud", value).apply()

    /** "slow", "normal" or "fast" */
    fun speed(c: Context): String = prefs(c).getString("speed", "normal") ?: "normal"
    fun setSpeed(c: Context, value: String) = prefs(c).edit().putString("speed", value).apply()

    fun rate(c: Context): Float = when (speed(c)) {
        "slow" -> 0.8f
        "fast" -> 1.25f
        else -> 1.0f
    }
}