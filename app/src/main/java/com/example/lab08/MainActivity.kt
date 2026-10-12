package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)

                TaskScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.observeAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.observeAsState("")
    val filter by viewModel.selectedFilter.observeAsState(FilterType.TODAS)
    val sort by viewModel.selectedSort.observeAsState(SortType.FECHA)
    val category by viewModel.selectedCategory.observeAsState("Todas")
    val isSyncing by viewModel.isSyncing.observeAsState(false)
    val notificationMsg by viewModel.notificationMessage.observeAsState(null)

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notificationMsg) {
        notificationMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Google Tasks Pro", fontWeight = FontWeight.Bold) },
                actions = {
                    // 10. Sincronización Cloud
                    IconButton(onClick = { viewModel.syncWithCloud() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar")
                        }
                    }
                    IconButton(onClick = { viewModel.deleteAllTasks() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Borrar todo")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Tarea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // 4. Barra de Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.searchQuery.value = it
                    viewModel.applyFiltersAndSort()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar tareas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Filtros y 9. Categorías (Chips horizontales)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filtro Estado
                items(FilterType.values().toList()) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = {
                            viewModel.selectedFilter.value = f
                            viewModel.applyFiltersAndSort()
                        },
                        label = { Text(f.name) }
                    )
                }

                item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }

                // Categorías
                val categories = listOf("Todas", "General", "Trabajo", "Estudio", "Hogar")
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = {
                            viewModel.selectedCategory.value = cat
                            viewModel.applyFiltersAndSort()
                        },
                        label = { Text(cat) },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // 5. Opciones de Ordenamiento
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ordenar por: ", style = MaterialTheme.typography.bodySmall)
                SortType.values().forEach { s ->
                    TextButton(onClick = {
                        viewModel.selectedSort.value = s
                        viewModel.applyFiltersAndSort()
                    }) {
                        Text(
                            text = s.name,
                            fontWeight = if (sort == s) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Lista de Tareas
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay tareas registradas", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para Agregar Tarea
    if (showAddDialog) {
        TaskFormDialog(
            title = "Nueva Tarea",
            onDismiss = { showAddDialog = false },
            onSave = { desc, prio, cat, recur ->
                viewModel.addTask(desc, prio, cat, recur)
                showAddDialog = false
            }
        )
    }

    // Diálogo para Editar Tarea (1. Editar tareas)
    taskToEdit?.let { task ->
        TaskFormDialog(
            title = "Editar Tarea",
            initialDesc = task.description,
            initialPriority = task.priority,
            initialCategory = task.category,
            initialRecurring = task.isRecurring,
            onDismiss = { taskToEdit = null },
            onSave = { desc, prio, cat, recur ->
                viewModel.updateTaskDetails(task, desc, prio, cat, recur)
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        Priority.ALTA -> Color(0xFFE53935)
        Priority.MEDIA -> Color(0xFFFB8C00)
        Priority.BAJA -> Color(0xFF43A047)
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (task.isRecurring) {
                        // 8. Indicador de Recurrente
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurrente",
                            modifier = Modifier.padding(start = 4.dp).size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // 7. Prioridad
                    Box(
                        modifier = Modifier
                            .background(priorityColor, shape = MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(task.priority.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    // 9. Categoría
                    Text("• ${task.category}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TaskFormDialog(
    title: String,
    initialDesc: String = "",
    initialPriority: Priority = Priority.MEDIA,
    initialCategory: String = "General",
    initialRecurring: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, Priority, String, Boolean) -> Unit
) {
    var desc by remember { mutableStateOf(initialDesc) }
    var priority by remember { mutableStateOf(initialPriority) }
    var category by remember { mutableStateOf(initialCategory) }
    var isRecurring by remember { mutableStateOf(initialRecurring) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") },
                    singleLine = true
                )

                Text("Prioridad:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Priority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }

                Text("Categoría:", style = MaterialTheme.typography.bodyMedium)
                val categories = listOf("General", "Trabajo", "Estudio", "Hogar")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Tarea Recurrente")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (desc.isNotBlank()) onSave(desc, priority, category, isRecurring) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}