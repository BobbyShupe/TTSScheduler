package com.example.ttsscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: return
        val id = intent.getIntExtra("reminder_id", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        // Start TTS in foreground service
        val serviceIntent = Intent(context, TTSService::class.java).apply {
            putExtra("text", text)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Reschedule for tomorrow
        if (id != -1) {
            AlarmScheduler.rescheduleNext(context, id, text, hour, minute)
        }
    }
}