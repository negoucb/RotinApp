package com.example.rotinapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tipo de recorrência da tarefa
enum class RecurrenceType {
    DAILY,        // Todo dia
    WEEKLY,       // Dias específicos da semana
    MONTHLY       // Dias específicos do mês
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val time: String,
    val description: String = "",
    val isDone: Boolean = false,
    val category: String = "",
    // Recorrência: "DAILY", "WEEKLY" ou "MONTHLY"
    val recurrenceType: String = RecurrenceType.DAILY.name,
    // Para WEEKLY: "1,3,5" = segunda, quarta, sexta (1=seg, 7=dom)
    // Para MONTHLY: "1,15,28" = dias do mês
    val recurrenceDays: String = ""
)