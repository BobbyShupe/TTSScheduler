package com.example.ttsscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderRepository.init(context)
            val reminders = ReminderRepository.getAll()
            for (reminder in reminders) {
                AlarmScheduler.schedule(context, reminder)
            }
        }
    }
}