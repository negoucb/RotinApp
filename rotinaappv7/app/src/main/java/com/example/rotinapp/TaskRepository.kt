package com.example.rotinapp

import com.example.rotinapp.database.TaskDao
import com.example.rotinapp.database.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {

    suspend fun insertAndGetId(task: TaskEntity): Long {
        return dao.insertTask(task)
    }

    suspend fun insert(task: TaskEntity) {
        dao.insertTask(task)
    }

    suspend fun update(task: TaskEntity) {
        dao.updateTask(task)
    }

    suspend fun delete(task: TaskEntity) {
        dao.deleteTask(task)
    }

    suspend fun getTaskById(id: Int): TaskEntity? {
        return dao.getTaskById(id)
    }

    fun getAll(): Flow<List<TaskEntity>> {
        return dao.getAllTasks()
    }

    fun getByCategory(category: String): Flow<List<TaskEntity>> {
        return dao.getTasksByCategory(category)
    }
}
