package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.PontoDatabase
import com.example.util.PontoReminderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = intent.action
                Log.d("ReminderReceiver", "onReceive triggered with action: $action")
                
                if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
                    rescheduleAllReminders(context)
                } else {
                    val label = intent.getStringExtra("label") ?: "Ponto"
                    val id = intent.getIntExtra("id", -1)
                    
                    Log.d("ReminderReceiver", "Firing reminder notification for label: $label (ID: $id)")
                    showNotification(context, label)
                    
                    if (id != -1) {
                        rescheduleNextOccurrence(context, id)
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error processing reminder broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllReminders(context: Context) {
        Log.d("ReminderReceiver", "Rescheduling all active reminders after device boot...")
        val db = PontoDatabase.getDatabase(context)
        val reminders = db.pontoDao().getAllRemindersList()
        for (reminder in reminders) {
            if (reminder.isEnabled) {
                PontoReminderHelper.scheduleReminder(context, reminder)
            }
        }
    }

    private suspend fun rescheduleNextOccurrence(context: Context, reminderId: Int) {
        Log.d("ReminderReceiver", "Rescheduling reminder $reminderId for the next day...")
        val db = PontoDatabase.getDatabase(context)
        val reminder = db.pontoDao().getReminderById(reminderId)
        if (reminder != null && reminder.isEnabled) {
            PontoReminderHelper.scheduleReminder(context, reminder)
        }
    }

    private fun showNotification(context: Context, label: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ponto_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lembretes de Ponto",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para não esquecer de bater o ponto"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Lembrete: Ponto de $label"
        val text = "Está na hora de registrar o seu ponto de $label! Abra o aplicativo para bater o ponto."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(label.hashCode(), notification)
    }
}
