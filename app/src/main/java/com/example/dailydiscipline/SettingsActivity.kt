package com.example.dailydiscipline

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var notificationSwitch: Switch
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var frequencySpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton

    private var startHour = 17
    private var startMinute = 0
    private var endHour = 23
    private var endMinute = 0
    private var frequencyMinutes = 30

    private val frequencyOptions = listOf(
        "Every 15 minutes" to 15,
        "Every 30 minutes" to 30,
        "Every 45 minutes" to 45,
        "Every 1 hour" to 60,
        "Every 2 hours" to 120
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        notificationSwitch = findViewById(R.id.notificationSwitch)
        startTimeButton = findViewById(R.id.startTimeButton)
        endTimeButton = findViewById(R.id.endTimeButton)
        frequencySpinner = findViewById(R.id.frequencySpinner)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        loadSettings()
        setupSpinner()
        updateTimeButtons()

        backButton.setOnClickListener {
            finish()
        }

        startTimeButton.setOnClickListener {
            showTimePicker(true)
        }

        endTimeButton.setOnClickListener {
            showTimePicker(false)
        }

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        notificationSwitch.isChecked = prefs.getBoolean(MainActivity.NOTIF_ENABLED_KEY, true)
        startHour = prefs.getInt(MainActivity.START_HOUR_KEY, 17)
        startMinute = prefs.getInt(MainActivity.START_MINUTE_KEY, 0)
        endHour = prefs.getInt(MainActivity.END_HOUR_KEY, 23)
        endMinute = prefs.getInt(MainActivity.END_MINUTE_KEY, 0)
        frequencyMinutes = prefs.getInt(MainActivity.FREQUENCY_KEY, 30)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            frequencyOptions.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        frequencySpinner.adapter = adapter

        val selectedIndex = frequencyOptions.indexOfFirst { it.second == frequencyMinutes }
        if (selectedIndex >= 0) {
            frequencySpinner.setSelection(selectedIndex)
        }

        frequencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                frequencyMinutes = frequencyOptions[position].second
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val hour = if (isStartTime) startHour else endHour
        val minute = if (isStartTime) startMinute else endMinute

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            if (isStartTime) {
                startHour = selectedHour
                startMinute = selectedMinute
            } else {
                endHour = selectedHour
                endMinute = selectedMinute
            }
            updateTimeButtons()
        }, hour, minute, false).show()
    }

    private fun updateTimeButtons() {
        startTimeButton.text = formatTime(startHour, startMinute)
        endTimeButton.text = formatTime(endHour, endMinute)
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

    private fun saveSettings() {
        // Validate times
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute

        if (startTotal >= endTotal) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putBoolean(MainActivity.NOTIF_ENABLED_KEY, notificationSwitch.isChecked)
            .putInt(MainActivity.START_HOUR_KEY, startHour)
            .putInt(MainActivity.START_MINUTE_KEY, startMinute)
            .putInt(MainActivity.END_HOUR_KEY, endHour)
            .putInt(MainActivity.END_MINUTE_KEY, endMinute)
            .putInt(MainActivity.FREQUENCY_KEY, frequencyMinutes)
            .apply()

        scheduleNotifications()

        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun scheduleNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel all existing alarms (up to 50)
        for (i in 0..50) {
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        if (!notificationSwitch.isChecked) {
            return
        }

        var requestCode = 0
        var currentHour = startHour
        var currentMinute = startMinute

        while (true) {
            val currentTotal = currentHour * 60 + currentMinute
            val endTotal = endHour * 60 + endMinute

            if (currentTotal > endTotal) break

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, currentHour)
                set(Calendar.MINUTE, currentMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, requestCode++, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            // Move to next notification time
            currentMinute += frequencyMinutes
            while (currentMinute >= 60) {
                currentMinute -= 60
                currentHour++
            }
        }
    }
}