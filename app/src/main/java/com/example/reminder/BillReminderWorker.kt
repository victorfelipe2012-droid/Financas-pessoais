package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.MainActivity
import com.example.data.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BillReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
            if (!isEnabled) {
                return Result.success()
            }
            checkAndNotifyBills(context)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val CHANNEL_ID = "privafin_bill_reminders"
        const val CHANNEL_NAME = "Lembretes de Contas"
        const val CHANNEL_DESC = "Avisos locais de contas a vencer e vencidas"
        const val UNIQUE_WORK_NAME = "PrivaFinDailyBillCheck"
        const val PREFS_NAME = "privafin_reminder_prefs"
        const val KEY_LAST_NOTIFIED_DAY = "last_notified_day_code"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"

        fun isRemindersEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
        }

        fun setRemindersEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
            if (enabled) {
                scheduleDailyBillCheck(context)
            } else {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            }
        }

        fun scheduleDailyBillCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            // Executa verificação diária
            val periodicRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        suspend fun checkAndNotifyBills(context: Context): Int {
            createNotificationChannel(context)

            val todayCode = getTodayCode()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_DAY, "")

            // Não duplicar notificações no mesmo dia para a mesma rotina periódica
            if (lastNotified == todayCode) {
                return 0
            }

            val database = AppDatabase.getDatabase(context)
            val pendingItems = database.financeDao().getPendingBillsAndApartmentSync()

            val now = System.currentTimeMillis()
            val endOfTomorrow = getEndOfTomorrowMillis()

            val overdueBills = pendingItems.filter { item ->
                val targetDate = item.dueDate ?: item.date
                targetDate < now
            }

            val dueSoonBills = pendingItems.filter { item ->
                val targetDate = item.dueDate ?: item.date
                targetDate in now..endOfTomorrow
            }

            val totalBillsToAlert = overdueBills.size + dueSoonBills.size
            if (totalBillsToAlert == 0) {
                return 0
            }

            val title = when {
                overdueBills.isNotEmpty() && dueSoonBills.isNotEmpty() ->
                    "Atenção: ${overdueBills.size} conta(s) vencida(s) e ${dueSoonBills.size} a vencer"
                overdueBills.isNotEmpty() ->
                    "Atenção: Você tem ${overdueBills.size} conta(s) vencida(s)"
                else ->
                    "Lembrete: Você tem ${dueSoonBills.size} conta(s) a vencer em breve"
            }

            // Notificação com privacidade: Não expõe valores na tela de bloqueio
            val secureSummary = "Abra o PrivaFin para conferir os vencimentos pendentes."

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_TAB", 0) // Abre o Dashboard/Resumo
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Versão pública para tela de bloqueio sem exibir valores nem dados privados
            val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("PrivaFin — Lembrete Financeiro")
                .setContentText("Você possui compromissos financeiros pendentes.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(secureSummary)
                .setPublicVersion(publicNotification)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val manager = NotificationManagerCompat.from(context)
            try {
                manager.notify(1001, notification)
                prefs.edit().putString(KEY_LAST_NOTIFIED_DAY, todayCode).apply()
            } catch (e: SecurityException) {
                // Permissão de notificação negada no Android 13+
            }

            return totalBillsToAlert
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun getTodayCode(): String {
            val cal = Calendar.getInstance()
            return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.DAY_OF_MONTH)}"
        }

        private fun getEndOfTomorrowMillis(): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            return cal.timeInMillis
        }
    }
}
