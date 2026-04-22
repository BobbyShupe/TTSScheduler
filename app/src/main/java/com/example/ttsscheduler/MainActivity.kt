package com.example.ttsscheduler

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var isMasterEnabled by remember { mutableStateOf(getMasterEnabledState(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    val refresh = { reminders = ReminderRepository.getAll() }

    val toggleMaster = { enabled: Boolean ->
        isMasterEnabled = enabled
        saveMasterEnabledState(context, enabled)
        if (enabled) {
            reminders.forEach { if (it.isEnabled) AlarmScheduler.schedule(context, it) }
        } else {
            reminders.forEach { AlarmScheduler.cancel(context, it.id) }
        }
    }

    val toggleReminder = { reminder: Reminder, enabled: Boolean ->
        val updated = reminder.copy(isEnabled = enabled)
        val currentList = ReminderRepository.getAll().toMutableList()
        val index = currentList.indexOfFirst { it.id == reminder.id }
        if (index != -1) {
            currentList[index] = updated
            ReminderRepository.saveAll(currentList)
            refresh()

            if (enabled && isMasterEnabled) {
                AlarmScheduler.schedule(context, updated)
            } else {
                AlarmScheduler.cancel(context, reminder.id)
            }
        }
    }

    val deleteReminder = { id: Int ->
        ReminderRepository.delete(id)
        AlarmScheduler.cancel(context, id)
        refresh()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TTS Daily Scheduler") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMasterEnabled) "Master ON" else "Master OFF",
                            color = if (isMasterEnabled) Color.Green else Color.Gray,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isMasterEnabled,
                            onCheckedChange = toggleMaster,
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
            if (isMasterEnabled) {
                FloatingActionButton(
                    onClick = {
                        editingReminder = null
                        showDialog = true
                    },
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No reminders yet.\nTap + to add one.",
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

                            // Individual Toggle
                            Switch(
                                checked = reminder.isEnabled,
                                onCheckedChange = { toggleReminder(reminder, it) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4CAF50)
                                )
                            )

                            // Edit Button
                            IconButton(onClick = {
                                editingReminder = reminder
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFBB86FC))
                            }

                            // Delete Button
                            IconButton(onClick = { deleteReminder(reminder.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showDialog) {
        var text by remember { mutableStateOf(editingReminder?.text ?: "") }
        val timePickerState = rememberTimePickerState(
            initialHour = editingReminder?.hour ?: 8,
            initialMinute = editingReminder?.minute ?: 0
        )

        AlertDialog(
            onDismissRequest = {
                showDialog = false
                editingReminder = null
            },
            containerColor = Color(0xFF1C1C1C),
            title = { Text(if (editingReminder == null) "New Daily Reminder" else "Edit Reminder", color = Color.White) },
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
                        val id = editingReminder?.id ?: ((reminders.maxOfOrNull { it.id } ?: -1) + 1)
                        val newReminder = Reminder(
                            id = id,
                            text = text,
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            isEnabled = editingReminder?.isEnabled ?: true
                        )

                        if (editingReminder == null) {
                            // Add new
                            ReminderRepository.add(newReminder)
                            if (isMasterEnabled && newReminder.isEnabled) {
                                AlarmScheduler.schedule(context, newReminder)
                            }
                        } else {
                            // Update existing
                            val currentList = ReminderRepository.getAll().toMutableList()
                            val index = currentList.indexOfFirst { it.id == id }
                            if (index != -1) {
                                currentList[index] = newReminder
                                ReminderRepository.saveAll(currentList)
                            }
                            // Re-schedule if needed
                            AlarmScheduler.cancel(context, id)
                            if (isMasterEnabled && newReminder.isEnabled) {
                                AlarmScheduler.schedule(context, newReminder)
                            }
                        }

                        refresh()
                        showDialog = false
                        editingReminder = null
                    }
                }) {
                    Text(if (editingReminder == null) "Add" else "Save", color = Color(0xFFBB86FC))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    editingReminder = null
                }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

// ======================
// Helper functions for master toggle
// ======================
private fun getMasterEnabledState(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("tts_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean("master_enabled", true)
}

private fun saveMasterEnabledState(context: android.content.Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences("tts_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean("master_enabled", enabled).apply()
}