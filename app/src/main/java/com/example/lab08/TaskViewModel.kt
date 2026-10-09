package com.example.lab08

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    // Contenedor mutable interno
    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    // Estado inmutable expuesto a la UI
    val tasks: LiveData<List<Task>> = _tasks

    init {
        // Carga inicial de tareas al instanciar el ViewModel
        viewModelScope.launch {
            _tasks.value = dao.getAllTasks()
        }
    }

    // Inserta una tarea y actualiza el LiveData en el hilo principal
    fun addTask(description: String) {
        val newTask = Task(description = description)
        viewModelScope.launch {
            dao.insertTask(newTask)
            _tasks.value = dao.getAllTasks()
        }
    }

    // Modifica el estado de completado y refresca la lista
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updatedTask)
            _tasks.value = dao.getAllTasks()
        }
    }

    // Borra todas las tareas de Room y vacía el estado
    fun deleteAllTasks() {
        viewModelScope.launch {
            dao.deleteAllTasks()
            _tasks.value = emptyList()
        }
    }
}