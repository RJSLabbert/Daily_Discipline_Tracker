package com.example.dailydiscipline

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TasksActivity : AppCompatActivity() {

    private lateinit var taskInput: EditText
    private lateinit var addButton: Button
    private lateinit var taskContainer: LinearLayout
    private lateinit var resetButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var prefs: SharedPreferences

    private val tasks = mutableListOf<Task>()

    data class Task(val name: String, var completed: Boolean = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        taskInput = findViewById(R.id.taskInput)
        addButton = findViewById(R.id.addButton)
        taskContainer = findViewById(R.id.taskContainer)
        resetButton = findViewById(R.id.resetButton)
        backButton = findViewById(R.id.backButton)

        loadTasks()

        backButton.setOnClickListener {
            finish()
        }

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

    private fun loadTasks() {
        tasks.clear()

        val namesString = prefs.getString(MainActivity.TASK_NAMES_KEY, "") ?: ""
        val statusString = prefs.getString(MainActivity.TASK_STATUS_KEY, "") ?: ""

        // Skip if empty or invalid
        if (namesString.isEmpty() || namesString.isBlank()) {
            refreshTaskViews()
            return
        }

        try {
            val names = namesString.split("|||").filter { it.isNotEmpty() }
            val statuses = if (statusString.isNotEmpty()) {
                statusString.split("|||")
            } else {
                names.map { "false" }
            }

            names.forEachIndexed { index, name ->
                if (name.isNotEmpty()) {
                    val completed = statuses.getOrNull(index)?.toBooleanStrictOrNull() ?: false
                    tasks.add(Task(name, completed))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Clear corrupted data
            prefs.edit()
                .putString(MainActivity.TASK_NAMES_KEY, "")
                .putString(MainActivity.TASK_STATUS_KEY, "")
                .apply()
        }

        refreshTaskViews()
    }
    private fun saveTasks() {
        val namesString = tasks.joinToString("|||") { it.name }
        val statusString = tasks.joinToString("|||") { it.completed.toString() }

        prefs.edit()
            .putString(MainActivity.TASK_NAMES_KEY, namesString)
            .putString(MainActivity.TASK_STATUS_KEY, statusString)
            .apply()
    }

    private fun addTask(name: String) {
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
}