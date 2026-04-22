package com.example.ttsscheduler

data class Reminder(
    val id: Int,
    val text: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true   // New field
)