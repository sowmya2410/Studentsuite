package com.example.studentsuite

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.toObject

// Task data class with an id to identify each task uniquely in Firestore
data class Task(val id: String = "", val task: String = "")

class ToDoListActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var tasksCollection: CollectionReference

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the user is logged in
        val userEmail = UserSession.userEmail
        if (userEmail == null) {
            // Handle the case when the user is not logged in
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
            finish()  // Close the activity
            return
        }

        // Set the collection path based on the authenticated user's email
        tasksCollection = firestore.collection("tasks")
            .document(userEmail)
            .collection("userTasks")

        setContent {
            var taskInput by remember { mutableStateOf("") }
            val tasks = remember { mutableStateListOf<Task>() }

            // Load tasks from Firestore
            LaunchedEffect(Unit) {
                // Listen to real-time changes in the user's task collection
                tasksCollection.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this@ToDoListActivity, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    tasks.clear()
                    snapshot?.documents?.mapNotNull { doc ->
                        val task = doc.toObject<Task>()
                        task?.copy(id = doc.id) // Set the Firestore document ID as the Task id
                    }?.let { tasks.addAll(it) }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("To-Do List") },
                        actions = {
                            IconButton(onClick = { /* Handle settings */ }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Task")
                            }
                        }
                    )
                },
                content = { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Task Input Section
                        OutlinedTextField(
                            value = taskInput,
                            onValueChange = { taskInput = it },
                            label = { Text("Add Task") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5)),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Task Button
                        Button(
                            onClick = {
                                if (taskInput.isNotEmpty()) {
                                    val newTask = Task(task = taskInput)
                                    // Add the task to Firestore using the authenticated user's email
                                    tasksCollection.add(newTask)
                                        .addOnSuccessListener {
                                            taskInput = ""
                                            Toast.makeText(this@ToDoListActivity, "Task added", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this@ToDoListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Add Task", color = Color.White)
                        }

                        // Task List Section
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasks) { task ->
                                TaskItem(task, onDelete = {
                                    tasksCollection.document(task.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(this@ToDoListActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this@ToDoListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                })
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun TaskItem(task: Task, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.task,
                    style = MaterialTheme.typography.bodyMedium, // Updated to use bodyMedium for text
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
