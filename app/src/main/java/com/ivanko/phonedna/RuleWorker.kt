package com.ivanko.phonedna

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RuleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val now = Date()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val rules = RuleStore.all(applicationContext).toMutableList()
        var changed = false
        rules.indices.forEach { i ->
            val rule = rules[i]
            if (rule.enabled && rule.trigger == "time" && rule.time == time && rule.lastRunDay != day) {
                notify(rule.title, "Сработало правило Phone DNA")
                rules[i] = rule.copy(lastRunDay = day)
                changed = true
            }
        }
        if (changed) RuleStore.save(applicationContext, rules)
        return Result.success()
    }

    private fun notify(title: String, message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("phone_dna_rules", "Phone DNA", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(applicationContext, "phone_dna_rules")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title).setContentText(message).setAutoCancel(true).build()
        try { manager.notify(title.hashCode(), notification) } catch (_: SecurityException) { }
    }
}
