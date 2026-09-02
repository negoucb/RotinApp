package com.example.rotinapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.rotinapp.database.RecurrenceType
import com.example.rotinapp.database.TaskDatabase
import com.example.rotinapp.database.TaskEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar

const val CHANNEL_ID = "rotinapp_tasks"
const val EXTRA_TASK_TITLE = "task_title"
const val EXTRA_TASK_ID = "task_id"

// ──────────────────────────────────────────────────────────────
// BroadcastReceiver: dispara quando o alarme chega
// ──────────────────────────────────────────────────────────────
class TaskAlarmReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val title  = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Tarefa"
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        showNotification(context, taskId, title)

        // Reagenda o próximo alarme para a recorrência da tarefa
        GlobalScope.launch(Dispatchers.IO) {
            val db   = TaskDatabase.getDatabase(context)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null && !task.isDone) {
                scheduleTaskNotification(context, task)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// BroadcastReceiver: reagenda todos os alarmes após reinicialização
// ──────────────────────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        GlobalScope.launch(Dispatchers.IO) {
            val db    = TaskDatabase.getDatabase(context)
            val tasks = db.taskDao().getAllTasksOnce()
            tasks.filter { !it.isDone }.forEach { scheduleTaskNotification(context, it) }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Canal de notificação (chame no onCreate do MainActivity)
// ──────────────────────────────────────────────────────────────
fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Lembretes de Tarefas",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Notificações para lembrar das suas tarefas"
        enableVibration(true)
        enableLights(true)
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

// ──────────────────────────────────────────────────────────────
// Exibe a notificação imediatamente
// ──────────────────────────────────────────────────────────────
fun showNotification(context: Context, taskId: Int, taskTitle: String) {
    val manager = context.getSystemService(NotificationManager::class.java)

    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, taskId, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("⏰ Hora da sua tarefa!")
        .setContentText(taskTitle)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    manager.notify(taskId, notification)
}

// ──────────────────────────────────────────────────────────────
// Calcula o próximo disparo respeitando recorrência
// Retorna null se a tarefa não deve mais disparar (ex.: WEEKLY sem dia elegível)
// ──────────────────────────────────────────────────────────────
private fun nextFireTime(task: TaskEntity): Long? {
    val timeParts = task.time.split(":").map { it.trim() }
    if (timeParts.size != 2) return null
    val hour   = timeParts[0].toIntOrNull() ?: return null
    val minute = timeParts[1].toIntOrNull() ?: return null

    val now = Calendar.getInstance()

    return when (task.recurrenceType) {

        RecurrenceType.DAILY.name -> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.timeInMillis
        }

        RecurrenceType.WEEKLY.name -> {
            // recurrenceDays = "1,3,5"  (1=seg … 7=dom, compatível com Calendar.DAY_OF_WEEK-1)
            val days = task.recurrenceDays.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .map { if (it == 7) Calendar.SUNDAY else it + 1 } // converte para Calendar day
                .sorted()
            if (days.isEmpty()) return null

            // Tenta encontrar o próximo dia elegível (hoje inclusive se o horário ainda não passou)
            for (offset in 0..7) {
                val candidate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dow = candidate.get(Calendar.DAY_OF_WEEK)
                if (dow in days && candidate.timeInMillis > now.timeInMillis) {
                    return candidate.timeInMillis
                }
            }
            null
        }

        RecurrenceType.MONTHLY.name -> {
            // recurrenceDays = "1,15,28"  (dias do mês)
            val days = task.recurrenceDays.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .sorted()
            if (days.isEmpty()) return null

            // Tenta até 2 meses à frente para encontrar o próximo dia elegível
            for (monthOffset in 0..2) {
                val base = Calendar.getInstance().apply {
                    add(Calendar.MONTH, monthOffset)
                }
                val maxDay = base.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in days) {
                    if (day > maxDay) continue
                    val candidate = Calendar.getInstance().apply {
                        set(Calendar.YEAR,         base.get(Calendar.YEAR))
                        set(Calendar.MONTH,        base.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY,  hour)
                        set(Calendar.MINUTE,       minute)
                        set(Calendar.SECOND,       0)
                        set(Calendar.MILLISECOND,  0)
                    }
                    if (candidate.timeInMillis > now.timeInMillis) return candidate.timeInMillis
                }
            }
            null
        }

        else -> {
            // Fallback: trata como DAILY
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.timeInMillis
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Agenda (ou reagenda) o alarme para uma tarefa
// ──────────────────────────────────────────────────────────────
fun scheduleTaskNotification(context: Context, task: TaskEntity) {
    val fireAt = nextFireTime(task) ?: return   // sem próximo horário elegível → não agenda
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
        putExtra(EXTRA_TASK_TITLE, task.title)
        putExtra(EXTRA_TASK_ID, task.id)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, task.id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
            }
        }
        else -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
    }
}

// ──────────────────────────────────────────────────────────────
// Cancela o alarme de uma tarefa
// ──────────────────────────────────────────────────────────────
fun cancelTaskNotification(context: Context, taskId: Int) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val intent = Intent(context, TaskAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, taskId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

fun cancelAllNotifications(context: Context, tasks: List<TaskEntity>) {
    tasks.forEach { cancelTaskNotification(context, it.id) }
}

fun scheduleAllNotifications(context: Context, tasks: List<TaskEntity>) {
    tasks.filter { !it.isDone }.forEach { scheduleTaskNotification(context, it) }
}

// ──────────────────────────────────────────────────────────────
// Helpers de permissão
// ──────────────────────────────────────────────────────────────
fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
    return true
}

fun openExactAlarmPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
