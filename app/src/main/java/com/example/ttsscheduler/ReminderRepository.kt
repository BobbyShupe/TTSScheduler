package com.example.ttsscheduler

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ReminderRepository {
    private const val PREF_NAME = "tts_reminders"
    private const val KEY = "reminders_list"
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getAll(): List<Reminder> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Reminder>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveAll(list: List<Reminder>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY, json).apply()
    }

    fun add(reminder: Reminder) {
        val current = getAll().toMutableList()
        current.add(reminder)
        saveAll(current)
    }

    fun delete(id: Int) {
        val filtered = getAll().filter { it.id != id }
        saveAll(filtered)
    }
}