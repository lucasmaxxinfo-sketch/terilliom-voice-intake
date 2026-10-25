package nz.terilliom.agent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("agent_reminders", "Agent reminders", NotificationManager.IMPORTANCE_HIGH))
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            android.app.Notification.Builder(context, "agent_reminders")
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
        val text = intent.getStringExtra("text") ?: "Your reminder is due."
        val open = Intent(context, MainActivity::class.java)
        val pending = android.app.PendingIntent.getActivity(context, 0, open, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Agent")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build())
    }
}
