package com.example.studentsuite

import android.app.TimePickerDialog
import android.os.Bundle
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.util.*

data class Event(
    val title: String,
    val description: String,
    val date: String,
    val time: String
)

class EventNewsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventNewsScreen()
        }
    }
}

@Composable
fun EventNewsScreen() {
    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventList by remember { mutableStateOf(mutableListOf<Event>()) }

    // Get the current context and initialize date/time
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    // Firestore instance and Auth instance
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Retrieve the user's email ID
    val emailId = UserSession.userEmail ?: return

    // Fetch events for the current user
    fun fetchEventsFromFirestore() {
        firestore.collection("events")
            .document(emailId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val events = document.get("events") as? List<Map<String, Any>> // Fetch the list of events

                    // Correct mapping of fetched events to Event model
                    eventList = events?.map { eventMap ->
                        Event(
                            title = eventMap["title"] as? String ?: "",
                            description = eventMap["description"] as? String ?: "",
                            date = eventMap["date"] as? String ?: "",
                            time = eventMap["time"] as? String ?: ""
                        )
                    }?.toMutableList() ?: mutableListOf() // Ensure we convert it to a mutable list

                    // After fetching, update the UI with the event list
                }
            }
            .addOnFailureListener { e ->
                // Handle failure to fetch events, perhaps log an error or show a message
            }
    }


    // Function to save event data to Firestore
    fun saveEventToFirestore() {
        if (eventTitle.isNotEmpty() && eventDescription.isNotEmpty() && eventDate.isNotEmpty() && eventTime.isNotEmpty()) {
            val newEvent = Event(eventTitle, eventDescription, eventDate, eventTime)

            // Add new event to the list
            eventList.add(newEvent)

            // Save the event list to Firestore under the user's document
            firestore.collection("events")
                .document(emailId)
                .set(mapOf("events" to eventList))
                .addOnSuccessListener {
                    // Clear the input fields after saving the event
                    eventTitle = ""
                    eventDescription = ""
                    eventDate = ""
                    eventTime = ""
                }
                .addOnFailureListener { e ->
                    // Handle failure to save event
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Input fields to add a new event
        OutlinedTextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { Text("Event Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = eventDescription,
            onValueChange = { eventDescription = it },
            label = { Text("Event Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Row for Date Picker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = eventDate,
                onValueChange = { eventDate = it },
                label = { Text("Event Date") },
                modifier = Modifier.weight(1f),
                enabled = false // Prevent manual editing
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                // Show DatePickerDialog
                android.app.DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        eventDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                    },
                    year,
                    month,
                    day
                ).show()
            }) {
                Text("Pick Date")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Row for Time Picker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = eventTime,
                onValueChange = { eventTime = it },
                label = { Text("Event Time") },
                modifier = Modifier.weight(1f),
                enabled = false // Prevent manual editing
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                // Show TimePickerDialog
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        val formattedTime = String.format(
                            "%02d:%02d %s",
                            if (selectedHour > 12) selectedHour - 12 else selectedHour,
                            selectedMinute,
                            if (selectedHour >= 12) "PM" else "AM"
                        )
                        eventTime = formattedTime
                    },
                    hour,
                    minute,
                    false // Use 12-hour format
                ).show()
            }) {
                Text("Pick Time")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Button to add the event
        Button(onClick = {
            saveEventToFirestore()
        }) {
            Text(text = "Add Event")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the list of events
        Text(text = "Upcoming Events", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Display each event in the list
        eventList.forEach { event ->
            EventItem(event)
        }
    }

    // Fetch events from Firestore when composable is launched
    LaunchedEffect(true) {
        fetchEventsFromFirestore()
    }
}

// Composable to display each event in the list
@Composable
fun EventItem(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Date: ${event.date}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Time: ${event.time}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Preview function
@Preview(showBackground = true)
@Composable
fun EventNewsPreview() {
    EventNewsScreen()
}
