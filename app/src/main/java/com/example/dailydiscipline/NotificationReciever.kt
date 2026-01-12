package com.example.dailydiscipline

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

        val enabled = prefs.getBoolean(MainActivity.NOTIF_ENABLED_KEY, true)
        if (!enabled) return

        val namesString = prefs.getString(MainActivity.TASK_NAMES_KEY, "") ?: ""
        val statusString = prefs.getString(MainActivity.TASK_STATUS_KEY, "") ?: ""

        // Skip if empty
        if (namesString.isEmpty() || namesString.isBlank()) return

        try {
            val names = namesString.split("|||").filter { it.isNotEmpty() }
            val statuses = if (statusString.isNotEmpty()) {
                statusString.split("|||")
            } else {
                names.map { "false" }
            }

            val incompleteTasks = mutableListOf<String>()
            names.forEachIndexed { index, name ->
                if (name.isNotEmpty()) {
                    val completed = statuses.getOrNull(index)?.toBooleanStrictOrNull() ?: false
                    if (!completed) {
                        incompleteTasks.add(name)
                    }
                }
            }

            if (incompleteTasks.isNotEmpty()) {
                val taskNames = incompleteTasks.joinToString(", ")

                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
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
                    NotificationManagerCompat.from(context).notify(1001, notification)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}