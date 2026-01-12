package com.example.dailydiscipline

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var taskInput: EditText
    private lateinit var addButton: Button
    private lateinit var taskContainer: LinearLayout
    private lateinit var resetButton: Button
    private lateinit var prefs: SharedPreferences

    private val tasks = mutableListOf<Task>()

    data class Task(val name: String, var completed: Boolean = false)

    companion object {
        const val CHANNEL_ID = "daily_discipline_channel"
        const val NOTIFICATION_ID = 1001
        const val PREFS_NAME = "DailyDisciplinePrefs"
        const val TASK_NAMES_KEY = "task_names"
        const val TASK_STATUS_KEY = "task_status"
        const val LAST_RESET_KEY = "lastReset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        taskInput = findViewById(R.id.taskInput)
        addButton = findViewById(R.id.addButton)
        taskContainer = findViewById(R.id.taskContainer)
        resetButton = findViewById(R.id.resetButton)

        createNotificationChannel()
        requestNotificationPermission()
        checkDailyReset()
        loadTasks()
        scheduleNotifications()

        addButton.setOnClickListener {
            val taskName = taskInput.text.toString().trim()
            if (taskName.isNotEmpty()) {
                addTask(taskName)
                taskInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter a task name", Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.setOnClickListener {
            resetAllTasks()
        }
    }

    private fun checkDailyReset() {
        val lastReset = prefs.getString(LAST_RESET_KEY, "") ?: ""
        val today = getToday()
        if (lastReset != today) {
            // Reset completion status for new day
            val names = prefs.getString(TASK_NAMES_KEY, "") ?: ""
            if (names.isNotEmpty()) {
                val count = names.split("|||").size
                val allFalse = (1..count).joinToString("|||") { "false" }
                prefs.edit()
                    .putString(TASK_STATUS_KEY, allFalse)
                    .putString(LAST_RESET_KEY, today)
                    .apply()
            }
        }
    }

    private fun getToday(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun loadTasks() {
        tasks.clear()

        val namesString = prefs.getString(TASK_NAMES_KEY, "") ?: ""
        val statusString = prefs.getString(TASK_STATUS_KEY, "") ?: ""

        if (namesString.isNotEmpty()) {
            val names = namesString.split("|||")
            val statuses = if (statusString.isNotEmpty()) {
                statusString.split("|||")
            } else {
                names.map { "false" }
            }

            names.forEachIndexed { index, name ->
                val completed = statuses.getOrNull(index)?.toBoolean() ?: false
                tasks.add(Task(name, completed))
            }
        }

        refreshTaskViews()
    }

    private fun saveTasks() {
        val namesString = tasks.joinToString("|||") { it.name }
        val statusString = tasks.joinToString("|||") { it.completed.toString() }

        prefs.edit()
            .putString(TASK_NAMES_KEY, namesString)
            .putString(TASK_STATUS_KEY, statusString)
            .apply()
    }

    private fun addTask(name: String) {
        // Don't allow ||| in task names as it's our separator
        val safeName = name.replace("|||", "---")
        val task = Task(safeName)
        tasks.add(task)
        saveTasks()
        refreshTaskViews()
    }

    private fun removeTask(index: Int) {
        if (index in tasks.indices) {
            tasks.removeAt(index)
            saveTasks()
            refreshTaskViews()
        }
    }

    private fun toggleTask(index: Int, completed: Boolean) {
        if (index in tasks.indices) {
            tasks[index].completed = completed
            saveTasks()
        }
    }

    private fun resetAllTasks() {
        tasks.forEach { it.completed = false }
        saveTasks()
        refreshTaskViews()
        Toast.makeText(this, "All tasks reset", Toast.LENGTH_SHORT).show()
    }

    private fun refreshTaskViews() {
        taskContainer.removeAllViews()

        tasks.forEachIndexed { index, task ->
            val taskView = createTaskView(task, index)
            taskContainer.addView(taskView)
        }
    }

    private fun createTaskView(task: Task, index: Int): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setPadding(16, 16, 16, 16)
            setBackgroundColor(if (task.completed) 0xFFE8F5E9.toInt() else 0xFFF5F5F5.toInt())
        }

        val checkBox = CheckBox(this).apply {
            isChecked = task.completed
            setOnCheckedChangeListener { _, isChecked ->
                toggleTask(index, isChecked)
                layout.setBackgroundColor(if (isChecked) 0xFFE8F5E9.toInt() else 0xFFF5F5F5.toInt())
            }
        }

        val textView = TextView(this).apply {
            text = task.name
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setPadding(16, 0, 16, 0)
        }

        val deleteButton = Button(this).apply {
            text = "X"
            textSize = 12f
            minimumWidth = 0
            minWidth = 0
            setPadding(24, 8, 24, 8)
            setOnClickListener {
                removeTask(index)
            }
        }

        layout.addView(checkBox)
        layout.addView(textView)
        layout.addView(deleteButton)

        return layout
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

    private fun scheduleNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel existing alarms first
        for (i in 0..12) {
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        var requestCode = 0

        // Schedule notifications from 5:00 PM to 11:00 PM (every 30 minutes)
        for (hour in 17..23) {
            for (minute in listOf(0, 30)) {
                if (hour == 23 && minute == 30) continue

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If time has passed today, schedule for tomorrow
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
                        } else {
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
            }
        }
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

            val namesString = prefs.getString(TASK_NAMES_KEY, "") ?: ""
            val statusString = prefs.getString(TASK_STATUS_KEY, "") ?: ""

            if (namesString.isEmpty()) return

            val names = namesString.split("|||")
            val statuses = if (statusString.isNotEmpty()) {
                statusString.split("|||")
            } else {
                names.map { "false" }
            }

            val incompleteTasks = mutableListOf<String>()
            names.forEachIndexed { index, name ->
                val completed = statuses.getOrNull(index)?.toBoolean() ?: false
                if (!completed) {
                    incompleteTasks.add(name)
                }
            }

            if (incompleteTasks.isNotEmpty()) {
                val taskNames = incompleteTasks.joinToString(", ")

                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Tasks Incomplete!")
                    .setContentText("${incompleteTasks.size} task(s) remaining")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("You have ${incompleteTasks.size} incomplete task(s):\n$taskNames"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // App will reschedule notifications when opened
        }
    }
}