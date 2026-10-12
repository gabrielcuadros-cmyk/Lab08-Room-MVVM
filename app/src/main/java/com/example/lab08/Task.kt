package com.example.lab08

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { ALTA, MEDIA, BAJA }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIA, // 7. Prioridades
    val category: String = "General",        // 9. Categorías
    val isRecurring: Boolean = false,        // 8. Tareas recurrentes
    val timestamp: Long = System.currentTimeMillis() // 5. Para ordenar por fecha
)