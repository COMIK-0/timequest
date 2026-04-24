package com.example.timequest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    val difficulty: String,
    val dueDate: Long? = null,
    val estimatedMinutes: Int,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val completedAt: Long? = null
)
