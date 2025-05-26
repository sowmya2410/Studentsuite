package com.example.studentsuite

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.util.*

data class AbsenceEntry(
    val date: String,
    val hoursAbsent: Int,
    val remainingHours: Int,
    val reason: String?
)

data class SubjectAttendance(
    val subjectName: String,
    var absences: MutableList<AbsenceEntry>,
    val maxLimit: Int
)

class AttendanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttendanceTracker()
        }
    }
}

@Composable
fun AttendanceTracker() {
    // States to store the user input and message
    var selectedDate by remember { mutableStateOf("") }
    var hourAbsent by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // List to store subject attendance data
    var subjects by remember { mutableStateOf(mutableListOf<SubjectAttendance>()) }

    // Get current user's email ID for storing data under their unique document
    val auth = FirebaseAuth.getInstance()
    val emailId = UserSession.userEmail ?: ""

    // Firestore instance
    val firestore = FirebaseFirestore.getInstance()

    // Calendar instance for date picker
    val calendar = Calendar.getInstance()

    // Date picker dialog
    val datePickerDialog = DatePickerDialog(
        LocalContext.current,
        { _, year, month, day ->
            selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Function to save attendance data to Firestore
    fun saveAttendanceToFirestore() {
        val absLimit = limit.toIntOrNull() ?: 0
        val absentHour = hourAbsent.toIntOrNull() ?: 0

        if (subject.isNotEmpty() && selectedDate.isNotEmpty()) {
            val existingSubject = subjects.find { it.subjectName == subject }
            if (existingSubject != null) {
                val remainingHours = existingSubject.maxLimit - existingSubject.absences.sumOf { it.hoursAbsent }
                if (remainingHours >= absentHour) {
                    existingSubject.absences.add(
                        AbsenceEntry(
                            date = selectedDate,
                            hoursAbsent = absentHour,
                            remainingHours = remainingHours - absentHour,
                            reason = if (reason.isNotEmpty()) reason else null
                        )
                    )
                    message = "Updated $subject: Added absence on $selectedDate. Remaining: ${remainingHours - absentHour} hours."
                } else {
                    message = "Cannot add more hours. Absence limit exceeded!"
                }
            } else {
                subjects.add(
                    SubjectAttendance(
                        subjectName = subject,
                        absences = mutableListOf(
                            AbsenceEntry(
                                date = selectedDate,
                                hoursAbsent = absentHour,
                                remainingHours = absLimit - absentHour,
                                reason = if (reason.isNotEmpty()) reason else null
                            )
                        ),
                        maxLimit = absLimit
                    )
                )
                message = "Added $subject on $selectedDate. Remaining: ${absLimit - absentHour} hours."
            }

            // Save to Firestore under email ID document
            firestore.collection("attendance")
                .document(emailId)
                .set(mapOf("subjects" to subjects))
                .addOnSuccessListener {
                    message = "Attendance saved successfully!"
                }
                .addOnFailureListener { e ->
                    message = "Failed to save attendance: ${e.message}"
                }
        } else {
            message = "Please enter a valid subject name and date."
        }
    }

    // Retrieve attendance data from Firestore based on email ID
    fun retrieveAttendanceFromFirestore() {
        firestore.collection("attendance")
            .document(emailId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val retrievedSubjects = document.get("subjects") as? List<Map<String, Any>>
                    subjects = retrievedSubjects?.map { subjectMap ->
                        val subjectName = subjectMap["subjectName"] as? String ?: ""
                        val maxLimit = subjectMap["maxLimit"] as? Int ?: 0
                        val absences = (subjectMap["absences"] as? List<Map<String, Any>>)?.map { absenceMap ->
                            AbsenceEntry(
                                date = absenceMap["date"] as? String ?: "",
                                hoursAbsent = absenceMap["hoursAbsent"] as? Int ?: 0,
                                remainingHours = absenceMap["remainingHours"] as? Int ?: 0,
                                reason = absenceMap["reason"] as? String
                            )
                        }?.toMutableList() ?: mutableListOf()
                        SubjectAttendance(subjectName, absences, maxLimit)
                    }?.toMutableList() ?: mutableListOf()
                }
            }
            .addOnFailureListener { e ->
                message = "Failed to retrieve attendance: ${e.message}"
            }
    }

    // Column for UI elements
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Date Picker Button
        Button(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (selectedDate.isEmpty()) "Pick a Date" else "Selected: $selectedDate")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hour Absent Input
        OutlinedTextField(
            value = hourAbsent,
            onValueChange = { hourAbsent = it },
            label = { Text("Hours Absent") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subject Input
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Limit Input
        OutlinedTextField(
            value = limit,
            onValueChange = { limit = it },
            label = { Text("Set Absence Limit (hours)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Optional Reason Input
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason for Absence (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = { saveAttendanceToFirestore() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display message
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display tracked subjects
        Text(text = "Tracked Subjects:")
        subjects.forEach { subjectData ->
            Button(
                onClick = {
                    message = "Subject: ${subjectData.subjectName}\n" +
                            subjectData.absences.joinToString("\n") {
                                "${it.date}: ${it.hoursAbsent} hours absent, Remaining: ${it.remainingHours} hours, Reason: ${it.reason ?: "N/A"}"
                            }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(text = subjectData.subjectName)
            }
        }
    }

    // Retrieve data from Firestore on composable start
    LaunchedEffect(true) {
        retrieveAttendanceFromFirestore()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AttendanceTracker()
}
