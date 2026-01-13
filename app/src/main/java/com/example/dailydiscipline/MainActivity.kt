package com.example.dailydiscipline

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var prefs: SharedPreferences
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var notificationStatus: TextView
    private lateinit var taskContainer: LinearLayout
    private lateinit var emptyMessage: TextView

    // Data class to hold task information
    data class Task(val name: String, var completed: Boolean = false)

    // List to store all tasks
    private val tasks = mutableListOf<Task>()

    companion object {
        // SharedPreferences keys for storing data
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
        const val LAST_RESET_KEY = "last_reset_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences for persistent storage
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Find and store references to all UI elements
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)
        notificationStatus = findViewById(R.id.notificationStatus)
        taskContainer = findViewById(R.id.taskContainer)
        emptyMessage = findViewById(R.id.emptyMessage)

        val tasksButton: Button = findViewById(R.id.tasksButton)
        val settingsButton: Button = findViewById(R.id.settingsButton)

        // Setup notification channel (required for Android 8.0+)
        createNotificationChannel()

        // Request notification permission (required for Android 13+)
        requestNotificationPermission()

        // Check if tasks need to be reset for new day
        checkAndResetDaily()

        // Navigate to Tasks management screen
        tasksButton.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }

        // Navigate to Settings screen
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        // Check for daily reset when app comes to foreground
        checkAndResetDaily()

        // Reload and display tasks
        loadTasks()

        // Update progress bar and notification status
        updateProgress()
        updateNotificationStatus()
    }

    /**
     * Loads tasks from SharedPreferences storage
     * Parses the stored string format back into Task objects
     */
    private fun loadTasks() {
        tasks.clear()

        val namesString = prefs.getString(TASK_NAMES_KEY, "") ?: ""
        val statusString = prefs.getString(TASK_STATUS_KEY, "") ?: ""

        // Skip if no tasks saved
        if (namesString.isEmpty() || namesString.isBlank()) {
            displayTasks()
            return
        }

        try {
            // Split stored strings by delimiter
            val names = namesString.split("|||").filter { it.isNotEmpty() }
            val statuses = if (statusString.isNotEmpty()) {
                statusString.split("|||")
            } else {
                names.map { "false" }
            }

            // Create Task objects from stored data
            names.forEachIndexed { index, name ->
                if (name.isNotEmpty()) {
                    val completed = statuses.getOrNull(index)?.toBooleanStrictOrNull() ?: false
                    tasks.add(Task(name, completed))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Update the UI with loaded tasks
        displayTasks()
    }

    /**
     * Saves current tasks list to SharedPreferences
     * Converts Task objects to string format for storage
     */
    private fun saveTasks() {
        // Join all task names with delimiter
        val namesString = tasks.joinToString("|||") { it.name }

        // Join all task statuses with delimiter
        val statusString = tasks.joinToString("|||") { it.completed.toString() }

        // Save to SharedPreferences
        prefs.edit()
            .putString(TASK_NAMES_KEY, namesString)
            .putString(TASK_STATUS_KEY, statusString)
            .apply()
    }

    /**
     * Creates and displays task views in the task container
     * Shows empty message if no tasks exist
     */
    private fun displayTasks() {
        // Clear existing views
        taskContainer.removeAllViews()

        // Show empty message if no tasks
        if (tasks.isEmpty()) {
            emptyMessage.visibility = View.VISIBLE
            taskContainer.visibility = View.GONE
            return
        }

        // Hide empty message and show task list
        emptyMessage.visibility = View.GONE
        taskContainer.visibility = View.VISIBLE

        // Create a view for each task
        tasks.forEachIndexed { index, task ->
            val taskView = createTaskView(task, index)
            taskContainer.addView(taskView)
        }
    }

    /**
     * Creates a single task view with styling based on completion status
     * @param task The task data to display
     * @param index The position of the task in the list
     * @return The created view
     */
    private fun createTaskView(task: Task, index: Int): View {
        // Main container for the task
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL

            // Set layout parameters with margins
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }

            setPadding(16, 16, 16, 16)

            // Different background for completed vs incomplete tasks
            if (task.completed) {
                setBackgroundResource(R.drawable.task_completed_background)
            } else {
                setBackgroundResource(R.drawable.card_background)
            }
        }

        // Status icon (checkmark or empty box)
        val statusIcon = TextView(this).apply {
            // Green checkmark for completed, empty box for incomplete
            text = if (task.completed) "✅" else "⬜"
            textSize = 24f

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Task name text
        val taskName = TextView(this).apply {
            text = task.name
            textSize = 16f

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            setPadding(16, 0, 16, 0)

            // Strikethrough text for completed tasks
            if (task.completed) {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTextColor(0xFF888888.toInt())
            } else {
                setTextColor(0xFF333333.toInt())
            }
        }

        // Action button (Done or Undo)
        val actionButton = Button(this).apply {
            text = if (task.completed) "Undo" else "Done"
            textSize = 12f

            // Smaller button
            minimumWidth = 0
            minWidth = 0
            minimumHeight = 0
            minHeight = 0
            setPadding(24, 8, 24, 8)

            // Different colors for different states
            if (task.completed) {
                setBackgroundColor(0xFF9E9E9E.toInt())  // Gray for undo
            } else {
                setBackgroundColor(0xFF4CAF50.toInt())  // Green for done
            }
            setTextColor(0xFFFFFFFF.toInt())  // White text

            setOnClickListener {
                if (task.completed) {
                    // Undo without confirmation
                    undoTask(index)
                } else {
                    // Show confirmation before marking done
                    showCompletionConfirmation(task, index)
                }
            }
        }

        // Add all elements to the layout
        layout.addView(statusIcon)
        layout.addView(taskName)
        layout.addView(actionButton)

        return layout
    }

    /**
     * Shows a confirmation dialog before marking a task as complete
     * @param task The task to be completed
     * @param index The position of the task in the list
     */
    private fun showCompletionConfirmation(task: Task, index: Int) {
        AlertDialog.Builder(this)
            .setTitle("✅ Complete Task")
            .setMessage("Mark \"${task.name}\" as completed?")
            .setPositiveButton("Yes, I'm Done!") { dialog, _ ->
                // User confirmed - mark task as complete
                completeTask(index)
                dialog.dismiss()
            }
            .setNegativeButton("Not Yet") { dialog, _ ->
                // User cancelled - do nothing
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Marks a task as completed and updates UI
     * @param index The position of the task to complete
     */
    private fun completeTask(index: Int) {
        if (index in tasks.indices) {
            // Update task status
            tasks[index].completed = true

            // Save to storage
            saveTasks()

            // Refresh UI
            displayTasks()
            updateProgress()

            // Show success message
            showSuccessMessage()
        }
    }

    /**
     * Marks a task as incomplete (undo completion)
     * @param index The position of the task to undo
     */
    private fun undoTask(index: Int) {
        if (index in tasks.indices) {
            // Update task status
            tasks[index].completed = false

            // Save to storage
            saveTasks()

            // Refresh UI
            displayTasks()
            updateProgress()
        }
    }

    /**
     * Shows a brief success animation/message when task is completed
     */
    private fun showSuccessMessage() {
        // Check if all tasks are done
        val allDone = tasks.isNotEmpty() && tasks.all { it.completed }

        if (allDone) {
            // Show special message when all tasks complete
            AlertDialog.Builder(this)
                .setTitle("🎉 Amazing!")
                .setMessage("You've completed all your tasks for today!\n\nGreat job staying disciplined!")
                .setPositiveButton("Thanks!") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }
    }

    /**
     * Checks if today is a new day and resets task completion if needed
     * This ensures users start fresh each day
     */
    private fun checkAndResetDaily() {
        // Get today's date as string (e.g., "2024-01-15")
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        // Get last reset date from storage
        val lastReset = prefs.getString(LAST_RESET_KEY, "") ?: ""

        // If different day, reset all tasks to incomplete
        if (today != lastReset) {
            val namesString = prefs.getString(TASK_NAMES_KEY, "") ?: ""

            if (namesString.isNotEmpty()) {
                // Count tasks and create "false" status for each
                val taskCount = namesString.split("|||").filter { it.isNotEmpty() }.size
                val resetStatuses = (1..taskCount).joinToString("|||") { "false" }

                // Save reset statuses and today's date
                prefs.edit()
                    .putString(TASK_STATUS_KEY, resetStatuses)
                    .putString(LAST_RESET_KEY, today)
                    .apply()
            } else {
                // No tasks, just save today's date
                prefs.edit()
                    .putString(LAST_RESET_KEY, today)
                    .apply()
            }
        }
    }

    /**
     * Updates the progress bar and text based on task completion
     */
    private fun updateProgress() {
        if (tasks.isEmpty()) {
            progressText.text = "0 / 0"
            progressBar.progress = 0
            return
        }

        // Count completed tasks
        val total = tasks.size
        val completed = tasks.count { it.completed }

        // Calculate percentage
        val percentage = if (total > 0) (completed * 100) / total else 0

        // Update UI
        progressText.text = "$completed / $total"
        progressBar.progress = percentage
    }

    /**
     * Updates the notification status text at bottom of screen
     */
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

    /**
     * Converts 24-hour time to 12-hour format with AM/PM
     */
    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Creates notification channel (required for Android 8.0+)
     */
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

    /**
     * Requests notification permission (required for Android 13+)
     */
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