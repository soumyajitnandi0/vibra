package com.example.classcrush.data.model

object ChatUtils {
    /** Stable chatId from two userIds (order-independent). */
    fun chatIdOf(uid1: String, uid2: String): String {
        val a = uid1.trim()
        val b = uid2.trim()
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }
}

