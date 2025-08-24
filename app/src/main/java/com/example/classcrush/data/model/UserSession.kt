package com.example.classcrush.data.model

import android.content.Context

object UserSession {
    private const val PREF = "session"
    private const val KEY_ID = "user_id"
    private const val KEY_NAME = "user_name"

    fun save(context: Context, userId: String, userName: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_ID, userId)
            .putString(KEY_NAME, userName)
            .apply()
    }

    fun id(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ID, "") ?: ""

    fun name(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_NAME, "") ?: ""
}

