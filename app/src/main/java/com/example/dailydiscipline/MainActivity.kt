package com.example.dailydiscipline

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var notificationStatus: TextView

    companion object {
        const val CHANNEL_ID = "daily_discipline_channel"
        const val PREFS_NAME = "DailyDisciplinePrefs"
        const val TASK_NAMES_KEY = "task_names"
        const val TASK_STATUS_KEY = "task_status"
        const val NOTIF_ENABLED_KEY = "notif_enabled"
        const val START_HOUR_KEY = "start_hour"
        const val START_MINUTE_KEY = "start_minute"
        const val END_HOUR_KEY = "end_hour"
        const val END_MINUTE_KEY = "end_minute"
        const val FREQUENCY_KEY = "frequency"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)
        notificationStatus = findViewById(R.id.notificationStatus)

        val tasksButton: Button = findViewById(R.id.tasksButton)
        val settingsButton: Button = findViewById(R.id.settingsButton)

        createNotificationChannel()
        requestNotificationPermission()

        tasksButton.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateProgress()
        updateNotificationStatus()
    }

    private fun updateProgress() {
        val namesString = prefs.getString(TASK_NAMES_KEY, "") ?: ""
        val statusString = prefs.getString(TASK_STATUS_KEY, "") ?: ""

        if (namesString.isEmpty() || namesString.isBlank()) {
            progressText.text = "No tasks yet"
            progressBar.progress = 0
            return
        }

        try {
            val names = namesString.split("|||").filter { it.isNotEmpty() }
            val statuses = if (statusString.isNotEmpty()) {
                statusString.split("|||")
            } else {
                names.map { "false" }
            }

            val total = names.size
            var completed = 0

            statuses.forEach { status ->
                if (status.toBooleanStrictOrNull() == true) {
                    completed++
                }
            }

            val percentage = if (total > 0) (completed * 100) / total else 0

            progressText.text = "$completed / $total tasks completed"
            progressBar.progress = percentage
        } catch (e: Exception) {
            e.printStackTrace()
            progressText.text = "No tasks yet"
            progressBar.progress = 0
        }
    }

    private fun updateNotificationStatus() {
        val enabled = prefs.getBoolean(NOTIF_ENABLED_KEY, true)
        val startHour = prefs.getInt(START_HOUR_KEY, 17)
        val startMinute = prefs.getInt(START_MINUTE_KEY, 0)
        val endHour = prefs.getInt(END_HOUR_KEY, 23)
        val endMinute = prefs.getInt(END_MINUTE_KEY, 0)

        if (enabled) {
            val startTime = formatTime(startHour, startMinute)
            val endTime = formatTime(endHour, endMinute)
            notificationStatus.text = "🔔 Notifications: $startTime - $endTime"
        } else {
            notificationStatus.text = "🔕 Notifications disabled"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Discipline Reminders"
            val descriptionText = "Reminders for incomplete tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}