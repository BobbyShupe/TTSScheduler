package com.example.ttsscheduler

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        ReminderRepository.init(this)

        // Exact alarm permission request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please allow exact alarms for reliable daily TTS", Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black,
                    surface = Color(0xFF1C1C1C),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    primary = Color(0xFFBB86FC)
                )
            ) {
                // Make navigation bar black
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
                    window.navigationBarColor = Color.Black.toArgb()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
                    }
                }

                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf(ReminderRepository.getAll()) }
    var isEnabled by remember { mutableStateOf(getEnabledState(context)) }
    var showDialog by remember { mutableStateOf(false) }

    val refresh = { reminders = ReminderRepository.getAll() }

    // Enable/Disable all reminders
    val toggleEnabled = { enabled: Boolean ->
        isEnabled = enabled
        saveEnabledState(context, enabled)

        if (enabled) {
            // Re-schedule all reminders
            reminders.forEach { reminder ->
                AlarmScheduler.schedule(context, reminder)
            }
        } else {
            // Cancel all alarms
            reminders.forEach { reminder ->
                AlarmScheduler.cancel(context, reminder.id)
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TTS Daily Scheduler") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black
                ),
                actions = {
                    // Toggle Switch in top bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEnabled) "ON" else "OFF",
                            color = if (isEnabled) Color.Green else Color.Gray,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = toggleEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Green,
                                checkedTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEnabled) {  // Only show + button when enabled
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = Color(0xFFBB86FC)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add reminder", tint = Color.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (reminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnabled) "No reminders yet.\nTap + to add one." else "Reminders are disabled.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(reminders) { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = String.format("%02d:%02d every day", reminder.hour, reminder.minute),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB0B0B0)
                                )
                            }
                            IconButton(onClick = {
                                ReminderRepository.delete(reminder.id)
                                AlarmScheduler.cancel(context, reminder.id)
                                refresh()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Reminder Dialog
    if (showDialog) {
        var text by remember { mutableStateOf("") }
        val timePickerState = rememberTimePickerState()

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("New Daily Reminder", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Text to speak", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        val newId = (reminders.maxOfOrNull { it.id } ?: -1) + 1
                        val reminder = Reminder(
                            id = newId,
                            text = text,
                            hour = timePickerState.hour,
                            minute = timePickerState.minute
                        )
                        ReminderRepository.add(reminder)
                        if (isEnabled) {
                            AlarmScheduler.schedule(context, reminder)
                        }
                        refresh()
                        showDialog = false
                    }
                }) {
                    Text("Add", color = Color(0xFFBB86FC))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

// Helper functions for saving toggle state
private fun getEnabledState(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("tts_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean("enabled", true)
}

private fun saveEnabledState(context: android.content.Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences("tts_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean("enabled", enabled).apply()
}