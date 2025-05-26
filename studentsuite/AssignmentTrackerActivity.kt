package com.example.studentsuite

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.Calendar

// Data model for Assignment with documentId
data class Assignment(
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    var isCompleted: Boolean = false,
    val documentId: String? = null // Added documentId field
)

class AssignmentTrackerActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AssignmentTrackerScreen(firestore, auth)
        }
    }
}

// Composable function for Assignment Tracker Screen
@Composable
fun AssignmentTrackerScreen(firestore: FirebaseFirestore, auth: FirebaseAuth) {
    var assignmentTitle by remember { mutableStateOf("") }
    var assignmentDescription by remember { mutableStateOf("") }
    var assignmentDueDate by remember { mutableStateOf("") }
    var assignmentList by remember { mutableStateOf(mutableListOf<Assignment>()) }

    // Calendar Picker Dialog state
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val userEmail = UserSession.userEmail
    if (userEmail == null) {
        // Handle the case when the user is not logged in
        Toast.makeText(context, "User is not logged in", Toast.LENGTH_SHORT).show()
        return
    }

    // Reference to the Firestore collection for assignments
    val assignmentsCollection = firestore.collection("users")
        .document(userEmail)
        .collection("assignments")

    // Fetch assignments from Firestore
    LaunchedEffect(userEmail) {
        assignmentsCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(context, "Error loading assignments: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            snapshot?.documents?.let { documents ->
                assignmentList = documents.mapNotNull { it.toObject<Assignment>() }.toMutableList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Input fields to add a new assignment
        OutlinedTextField(
            value = assignmentTitle,
            onValueChange = { assignmentTitle = it },
            label = { Text("Assignment Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = assignmentDescription,
            onValueChange = { assignmentDescription = it },
            label = { Text("Assignment Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = assignmentDueDate,
                onValueChange = { assignmentDueDate = it },
                label = { Text("Due Date") },
                modifier = Modifier.weight(1f),
                enabled = false // Disable direct text editing for this field
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                // Show DatePickerDialog
                android.app.DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        assignmentDueDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                    },
                    year,
                    month,
                    day
                ).show()
            }) {
                Text("Pick Date")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Button to add the assignment
        Button(onClick = {
            if (assignmentTitle.isNotEmpty() && assignmentDescription.isNotEmpty() && assignmentDueDate.isNotEmpty()) {
                val newAssignment = Assignment(
                    assignmentTitle,
                    assignmentDescription,
                    assignmentDueDate
                )

                // Add the assignment to Firestore and get the document ID
                assignmentsCollection.add(newAssignment)
                    .addOnSuccessListener { documentReference ->
                        // Store the document ID in the new assignment object
                        val updatedAssignment = newAssignment.copy(documentId = documentReference.id)

                        // Update the assignment with the documentId
                        assignmentsCollection.document(documentReference.id).set(updatedAssignment)

                        // Clear input fields after adding the assignment
                        assignmentTitle = ""
                        assignmentDescription = ""
                        assignmentDueDate = ""
                        Toast.makeText(context, "Assignment added", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error adding assignment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }) {
            Text(text = "Add Assignment")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the list of assignments
        Text(text = "Your Assignments", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Display each assignment in the list
        assignmentList.forEach { assignment ->
            AssignmentItem(assignment, firestore)
        }
    }
}

// Composable to display each assignment in the list
@Composable
fun AssignmentItem(
    assignment: Assignment,
    firestore: FirebaseFirestore
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = assignment.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Due Date: ${assignment.dueDate}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Description: ${assignment.description}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Checkbox to mark the assignment as completed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = assignment.isCompleted,
                    onCheckedChange = { checked ->
                        val updatedAssignment = assignment.copy(isCompleted = checked)

                        // Use the documentId to update the specific task in Firestore
                        if (assignment.documentId != null) {
                            firestore.collection("users")
                                .document(UserSession.userEmail!!)
                                .collection("assignments")
                                .document(assignment.documentId)
                                .set(updatedAssignment)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Assignment updated", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error updating assignment: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (assignment.isCompleted) "Completed" else "Not Completed")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AssignmentTrackerPreview() {
    AssignmentTrackerScreen(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
}
