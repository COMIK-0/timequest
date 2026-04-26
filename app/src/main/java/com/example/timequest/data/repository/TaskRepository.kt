package com.example.timequest.data.repository

import com.example.timequest.data.local.TaskDao
import com.example.timequest.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getActiveTasks(): Flow<List<TaskEntity>> = taskDao.getActiveTasks()

    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()

    suspend fun getTaskById(taskId: Long): TaskEntity? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun updateTasks(tasks: List<TaskEntity>) {
        tasks.forEach { taskDao.updateTask(it) }
    }

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
}
