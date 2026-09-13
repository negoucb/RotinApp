package com.example.rotinapp.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY time ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // Versão suspend (sem Flow) usada pelo BootReceiver para reagendar após reinicialização
    @Query("SELECT * FROM tasks ORDER BY time ASC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY time ASC")
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): TaskEntity?
}