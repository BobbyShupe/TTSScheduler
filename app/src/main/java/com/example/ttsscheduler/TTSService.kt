package com.example.ttsscheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

class TTSService : Service() {

    private var tts: TextToSpeech? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("text") ?: return START_NOT_STICKY

        // Foreground service notification (required on Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "tts_channel"
            val channel = NotificationChannel(
                channelId, "TTS Reminders", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

            val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("Speaking Reminder")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

            // 🔥 Critical fix for Android 14+: pass the foreground service type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1001,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(1001, notification)
            }
        } else {
            // Pre-Oreo: no foreground required
            startForeground(1001, Notification())
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { stopSelf() }
                    override fun onError(utteranceId: String?) { stopSelf() }
                    override fun onError(utteranceId: String?, errorCode: Int) { stopSelf() }
                })

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "reminder")
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "reminder")
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) stopForeground(true)
        super.onDestroy()
    }
}