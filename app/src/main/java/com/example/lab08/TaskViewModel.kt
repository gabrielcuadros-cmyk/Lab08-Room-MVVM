package com.example.lab08

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FilterType { TODAS, PENDIENTES, COMPLETADAS }
enum class SortType { FECHA, NOMBRE, ESTADO, PRIORIDAD }

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _rawTasks = MutableLiveData<List<Task>>(emptyList())

    // Estados de UI
    val searchQuery = MutableLiveData("")
    val selectedFilter = MutableLiveData(FilterType.TODAS)
    val selectedSort = MutableLiveData(SortType.FECHA)
    val selectedCategory = MutableLiveData("Todas")

    val isSyncing = MutableLiveData(false)
    val notificationMessage = MutableLiveData<String?>(null)

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> = _tasks

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _rawTasks.value = dao.getAllTasks()
            applyFiltersAndSort()
        }
    }

    fun applyFiltersAndSort() {
        var result = _rawTasks.value ?: emptyList()

        // 4. Buscar tareas
        val query = searchQuery.value.orEmpty().trim()
        if (query.isNotEmpty()) {
            result = result.filter { it.description.contains(query, ignoreCase = true) }
        }

        // 3. Filtrar tareas
        result = when (selectedFilter.value) {
            FilterType.PENDIENTES -> result.filter { !it.isCompleted }
            FilterType.COMPLETADAS -> result.filter { it.isCompleted }
            else -> result
        }

        // 9. Filtrar por categoría
        val cat = selectedCategory.value.orEmpty()
        if (cat != "Todas") {
            result = result.filter { it.category == cat }
        }

        // 5. Ordenar tareas
        result = when (selectedSort.value) {
            SortType.NOMBRE -> result.sortedBy { it.description.lowercase() }
            SortType.ESTADO -> result.sortedBy { it.isCompleted }
            SortType.PRIORIDAD -> result.sortedBy { it.priority.ordinal }
            else -> result.sortedByDescending { it.timestamp }
        }

        _tasks.value = result
    }

    fun addTask(description: String, priority: Priority, category: String, isRecurring: Boolean) {
        if (description.isBlank()) return
        val newTask = Task(
            description = description,
            priority = priority,
            category = category,
            isRecurring = isRecurring
        )
        viewModelScope.launch {
            dao.insertTask(newTask)
            loadTasks()
            // 6. Simulación de Notificación
            notificationMessage.value = "Recordatorio configurado para: \"$description\""
        }
    }

    // 1. Editar tareas
    fun updateTaskDetails(task: Task, newDesc: String, newPriority: Priority, newCategory: String, newRecurring: Boolean) {
        viewModelScope.launch {
            val updated = task.copy(
                description = newDesc,
                priority = newPriority,
                category = newCategory,
                isRecurring = newRecurring
            )
            dao.updateTask(updated)
            loadTasks()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updated)

            // 8. Tareas recurrentes: si era recurrente y se completa, re-crea una pendiente automáticamente
            if (!task.isCompleted && task.isRecurring) {
                val recurringTask = Task(
                    description = "${task.description} (Recurrente)",
                    priority = task.priority,
                    category = task.category,
                    isRecurring = true
                )
                dao.insertTask(recurringTask)
            }
            loadTasks()
        }
    }

    // 2. Eliminar tarea individual
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
            loadTasks()
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            dao.deleteAllTasks()
            loadTasks()
        }
    }

    // 10. Sincronización en la Nube (+2 pts)
    fun syncWithCloud() {
        viewModelScope.launch {
            isSyncing.value = true
            delay(1500) // Simula la conexión a API Cloud Firebase/REST
            isSyncing.value = false
            notificationMessage.value = "¡Sincronización con la nube exitosa!"
        }
    }

    fun clearNotification() {
        notificationMessage.value = null
    }
}