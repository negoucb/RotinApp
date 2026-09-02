package com.example.rotinapp.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rotinapp.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TaskDatabase.getDatabase(application).taskDao()
    private val repository = TaskRepository(dao)

    val allTasks: Flow<List<TaskEntity>> = repository.getAll()

    // Insere e retorna o id gerado pelo Room (necessário para agendar notificação com id correto)
    suspend fun insertAndGetId(
        title: String,
        time: String,
        description: String = "",
        category: String = "",
        recurrenceType: String = RecurrenceType.DAILY.name,
        recurrenceDays: String = ""
    ): TaskEntity {
        val task = TaskEntity(
            title = title,
            time = time,
            description = description,
            isDone = false,
            category = category,
            recurrenceType = recurrenceType,
            recurrenceDays = recurrenceDays
        )
        val id = repository.insertAndGetId(task)
        return task.copy(id = id.toInt())
    }

    fun insert(
        title: String,
        time: String,
        description: String = "",
        category: String = "",
        recurrenceType: String = RecurrenceType.DAILY.name,
        recurrenceDays: String = ""
    ) {
        viewModelScope.launch {
            repository.insert(
                TaskEntity(
                    title = title,
                    time = time,
                    description = description,
                    isDone = false,
                    category = category,
                    recurrenceType = recurrenceType,
                    recurrenceDays = recurrenceDays
                )
            )
        }
    }

    fun update(task: TaskEntity) {
        viewModelScope.launch { repository.update(task) }
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch {
            repository.update(task.copy(isDone = !task.isDone))
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch { repository.delete(task) }
    }

    suspend fun getTaskById(id: Int): TaskEntity? {
        return repository.getTaskById(id)
    }
}
