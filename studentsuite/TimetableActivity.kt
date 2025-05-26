package com.example.studentsuite

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

data class TimetableEntry(
    val day: String,
    val time: String,
    val subject: String,
    val userEmail: String // Include userEmail in each entry to link to the user
)

class TimetableActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimetableScreen()
        }
    }
}
@Composable
fun TimetableScreen() {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var day by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var timetable by remember { mutableStateOf<List<TimetableEntry>>(emptyList()) }

    // Fetch the current user
    val userEmail = UserSession.userEmail ?: ""
    val context = LocalContext.current

    // Check if the user is logged in
    if (userEmail.isEmpty()) {
        // Show a message or a login screen if no user is logged in
        Text(
            text = "Please log in to view and manage your timetable.",
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        return
    }

    // Fetch timetable from Firestore for the current user
    LaunchedEffect(userEmail) {
        firestore.collection("timetable")
            .whereEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { result ->
                timetable = result.map { document ->
                    val day = document.getString("day") ?: ""
                    val time = document.getString("time") ?: ""
                    val subject = document.getString("subject") ?: ""
                    TimetableEntry(day, time, subject, userEmail)
                }
            }
            .addOnFailureListener {
                // Show toast inside the composable scope using LocalContext
                Toast.makeText(context, "Failed to load timetable.", Toast.LENGTH_SHORT).show()
            }
    }

    // Group timetable entries by day
    val groupedTimetable = timetable.groupBy { it.day }

    // Days of the week for dropdown menu
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var isDayDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Button to pick day using dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    isDayDropdownExpanded = !isDayDropdownExpanded
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(text = if (day.isEmpty()) "Pick Day" else "Day: $day")
            }
            DropdownMenu(
                expanded = isDayDropdownExpanded,
                onDismissRequest = { isDayDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                daysOfWeek.forEach { dayOption ->
                    DropdownMenuItem(
                        text = { Text(dayOption) },
                        onClick = {
                            day = dayOption
                            isDayDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Button to pick time
        // Local context reference inside composable
        Button(onClick = {
            showTimePicker(context) { pickedTime -> time = pickedTime }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = if (time.isEmpty()) "Pick Time" else "Time: $time")
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Input for subject
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Button to add timetable entry
        Button(onClick = {
            if (day.isNotEmpty() && time.isNotEmpty() && subject.isNotEmpty()) {
                val newEntry = TimetableEntry(day, time, subject, userEmail)

                // Add timetable entry to Firestore
                firestore.collection("timetable")
                    .add(newEntry)
                    .addOnSuccessListener {
                        // Clear fields after adding
                        day = ""
                        time = ""
                        subject = ""
                        // Re-fetch timetable to update UI
                        fetchTimetable(firestore, userEmail) { updatedTimetable ->
                            timetable = updatedTimetable
                        }
                    }
                    .addOnFailureListener {
                        // Show toast inside the composable scope using LocalContext
                        Toast.makeText(context, "Failed to add timetable entry.", Toast.LENGTH_SHORT).show()
                    }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Add to Timetable")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Display the timetable
        Text(text = "Timetable", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            groupedTimetable.keys.sorted().forEach { dayKey ->
                item {
                    Text(
                        text = dayKey,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(groupedTimetable[dayKey]?.sortedBy { it.time } ?: emptyList()) { entry ->
                    TimetableEntryView(entry)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Fetch timetable from Firestore
fun fetchTimetable(firestore: FirebaseFirestore, userEmail: String, onTimetableFetched: (List<TimetableEntry>) -> Unit) {
    firestore.collection("timetable")
        .whereEqualTo("userEmail", userEmail)
        .get()
        .addOnSuccessListener { result ->
            val timetable = result.map { document ->
                val day = document.getString("day") ?: ""
                val time = document.getString("time") ?: ""
                val subject = document.getString("subject") ?: ""
                TimetableEntry(day, time, subject, userEmail)
            }
            onTimetableFetched(timetable)
        }
}


@Composable
fun TimetableEntryView(entry: TimetableEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Time: ${entry.time}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Subject: ${entry.subject}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Helper function to show time picker dialog
fun showTimePicker(context: Context, onTimePicked: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _, hour, minute ->
            val timeString = String.format("%02d:%02d", hour, minute)
            onTimePicked(timeString)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

@Preview(showBackground = true)
@Composable
fun TimetableScreenPreview() {
    TimetableScreen()
}
