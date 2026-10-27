package au.edu.canberra.team48.shoppinglist.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.edu.canberra.team48.shoppinglist.MainActivity
import au.edu.canberra.team48.shoppinglist.R
import au.edu.canberra.team48.shoppinglist.data.Prefs
import au.edu.canberra.team48.shoppinglist.domain.Reminders
import java.util.concurrent.TimeUnit

object Notifications {
    private const val CHANNEL = "shopping_reminders"
    private const val REMINDER_ID = 48

    /** minSdk is 26, so notification channels always exist. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Shopping reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "A reminder when you have not been shopping for a while"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked by canPost
    fun showReminder(context: Context, waiting: Int) {
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to shop?")
            .setContentText(if (waiting == 1) "1 thing on your list" else "$waiting things on your list")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(REMINDER_ID, n)
    }
}

/** Checks once a day whether it has been too long since the last shop. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val p = Prefs(applicationContext)
        val due = Reminders.shouldRemind(
            enabled = p.remindOn,
            days = p.remindDays,
            lastShopAt = p.lastShopAt,
            oldestWaitingAt = p.oldestWaitingAt,
            waiting = p.waitingCount,
            now = System.currentTimeMillis(),
        )
        if (due) Notifications.showReminder(applicationContext, p.waitingCount)
        return Result.success()
    }

    companion object {
        private const val WORK = "shopping_reminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
        }
    }
}
