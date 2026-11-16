package com.example.rawal.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_DURATION
        val lang = intent.getStringExtra(EXTRA_LANG) ?: "en"
        val isFa = lang == "fa"

        ReminderScheduler.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = when (type) {
            TYPE_EOD -> if (isFa) "مرور پایان روز روال" else "RAWAL End-of-Day Review"
            else -> if (isFa) "ثبت زمان فعالیت‌های امروز" else "RAWAL Activity Duration Check"
        }
        val body = when (type) {
            TYPE_EOD -> if (isFa) "زمان ارزیابی رضایت، حال‌وهوا و یادداشت‌های امروز فرا رسیده است." else "Take a minute to record your daily satisfaction, mood, and notes."
            else -> if (isFa) "آیا مدت‌زمان تمام فعالیت‌های امروز را ثبت کرده‌اید؟" else "Check if any completed activities are missing manual duration entries."
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            type.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            if (type == TYPE_EOD) 1002 else 1001,
            notification
        )
    }

    companion object {
        const val CHANNEL_ID = "rawal_reminders"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_LANG = "lang"
        const val TYPE_DURATION = "duration"
        const val TYPE_EOD = "eod"
    }
}

object ReminderScheduler {
    private const val REQ_DURATION = 4001
    private const val REQ_EOD = 4002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "RAWAL Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for activity durations and end-of-day review"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun syncReminders(
        context: Context,
        enabled: Boolean,
        langCode: String,
        durationHour: Int,
        durationMinute: Int,
        eodHour: Int,
        eodMinute: Int
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!enabled) {
            alarmManager.cancel(buildPendingIntent(context, REQ_DURATION, ReminderReceiver.TYPE_DURATION, langCode))
            alarmManager.cancel(buildPendingIntent(context, REQ_EOD, ReminderReceiver.TYPE_EOD, langCode))
            return
        }

        scheduleDaily(
            alarmManager,
            durationHour,
            durationMinute,
            buildPendingIntent(context, REQ_DURATION, ReminderReceiver.TYPE_DURATION, langCode)
        )
        scheduleDaily(
            alarmManager,
            eodHour,
            eodMinute,
            buildPendingIntent(context, REQ_EOD, ReminderReceiver.TYPE_EOD, langCode)
        )
    }

    private fun scheduleDaily(
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        pendingIntent: PendingIntent
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        type: String,
        langCode: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_TYPE, type)
            putExtra(ReminderReceiver.EXTRA_LANG, langCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
