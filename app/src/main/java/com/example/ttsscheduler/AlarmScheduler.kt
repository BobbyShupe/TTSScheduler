package com.example.ttsscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        val triggerTime = calculateTriggerTime(reminder.hour, reminder.minute)
        scheduleInternal(context, reminder.id, reminder.text, reminder.hour, reminder.minute, triggerTime)
    }

    fun rescheduleNext(context: Context, id: Int, text: String, hour: Int, minute: Int) {
        val triggerTime = calculateNextDayTriggerTime(hour, minute)
        scheduleInternal(context, id, text, hour, minute, triggerTime)
    }

    private fun calculateTriggerTime(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun calculateNextDayTriggerTime(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun scheduleInternal(
        context: Context,
        id: Int,
        text: String,
        hour: Int,
        minute: Int,
        triggerTime: Long
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", id)
            putExtra("text", text)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancel(context: Context, id: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}