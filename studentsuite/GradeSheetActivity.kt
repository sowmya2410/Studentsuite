package com.example.studentsuite

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.studentsuite.Subject



data class Semester(val name: String, val subjects: List<Subject> = emptyList(), val cgpa: Float = 0f)

class GradeSheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GradeSheetApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeSheetApp() {
    var semesterName by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf<Semester?>(null) }
    var subjectName by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var collectiveCGPA by remember { mutableStateOf<Float?>(null) }
    var semestersList by remember { mutableStateOf(listOf<Semester>()) }
    val context = LocalContext.current
    var isSemesterDropdownExpanded by remember { mutableStateOf(false) }
    var isGradeDropdownExpanded by remember { mutableStateOf(false) }

    // Firebase instances
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Get current user's email
    val user = auth.currentUser
    val emailId = UserSession.userEmail ?: ""

    // Grades for dropdown
    val grades = listOf("O", "A+", "A", "B+", "B", "C", "F")
    var selectedGrade by remember { mutableStateOf(grades[0]) } // Default grade

    // Fetch Semesters from Firestore
    LaunchedEffect(emailId) {
        if (emailId.isNotEmpty()) {
            fetchSemestersFromFirestore(emailId) { fetchedSemesters ->
                semestersList = fetchedSemesters
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Grade Sheet (Semester-wise)",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Add or Select Semester
        TextField(
            value = semesterName,
            onValueChange = { semesterName = it },
            label = { Text("Semester Name (e.g., Semester 1)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (semesterName.isNotBlank() && semestersList.none { it.name == semesterName }) {
                // Add semester to Firestore
                val newSemester = Semester(name = semesterName)
                saveSemesterToFirestore(emailId, newSemester)
                semestersList = semestersList + newSemester
                semesterName = "" // Clear the input field
            } else {
                Toast.makeText(context, "Invalid or Duplicate Semester Name", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Add Semester")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Select Semester Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    isSemesterDropdownExpanded = !isSemesterDropdownExpanded
                    isGradeDropdownExpanded = false // Close grade dropdown if open
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(text = selectedSemester?.name ?: "Select Semester")
            }
            DropdownMenu(
                expanded = isSemesterDropdownExpanded,
                onDismissRequest = { isSemesterDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                semestersList.forEach { semester ->
                    DropdownMenuItem(
                        text = { Text(text = semester.name) },
                        onClick = {
                            selectedSemester = semester
                            isSemesterDropdownExpanded = false // Close the dropdown after selection
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSemester != null) {
            Text(text = "Selected Semester: ${selectedSemester!!.name}")
            Spacer(modifier = Modifier.height(8.dp))

            // Input Fields for Subject
            TextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = credits,
                onValueChange = { credits = it },
                label = { Text("Credits (Numeric)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Grade label and selection dropdown
            Text(text = "Select Grade", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        isGradeDropdownExpanded = !isGradeDropdownExpanded
                        isSemesterDropdownExpanded = false // Close semester dropdown if open
                    },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(text = selectedGrade) // Display selected grade
                }

                DropdownMenu(
                    expanded = isGradeDropdownExpanded,
                    onDismissRequest = { isGradeDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    grades.forEach { gradeOption ->
                        DropdownMenuItem(
                            text = { Text(gradeOption) },
                            onClick = {
                                selectedGrade = gradeOption // Update selected grade
                                isGradeDropdownExpanded = false // Close dropdown
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Subject to Selected Semester
            Button(onClick = {
                val creditValue = credits.toIntOrNull()
                if (subjectName.isNotBlank() && creditValue != null && creditValue > 0 && selectedGrade.isNotBlank()) {
                    selectedSemester?.let {
                        val updatedSubjects = it.subjects + Subject(subjectName, creditValue, selectedGrade)
                        val updatedCGPA = calculateCGPA(updatedSubjects)

                        // Update Firestore with the new subject and CGPA
                        saveSemesterToFirestore(emailId, it.copy(subjects = updatedSubjects, cgpa = updatedCGPA))

                        semestersList = semestersList.map { semester ->
                            if (semester.name == it.name) {
                                semester.copy(subjects = updatedSubjects, cgpa = updatedCGPA)
                            } else {
                                semester
                            }
                        }
                        selectedSemester = semestersList.first { it.name == it.name }
                        subjectName = ""
                        credits = ""
                        selectedGrade = grades[0]
                    } ?: Toast.makeText(context, "Please select a valid semester", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Add Subject")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Semesters
        Text(
            text = "Semesters:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        semestersList.forEach { semester ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${semester.name} - GPA: ${semester.cgpa}",
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Delete Button for each semester
                Button(
                    onClick = {
                        deleteSemesterFromFirestore(emailId, semester.name) {
                            semestersList = semestersList.filter { it.name != semester.name }
                            selectedSemester = null
                        }
                    }
                ) {
                    Text("Delete Semester")
                }

                semester.subjects.forEach { subject ->
                    Text(
                        text = "  ${subject.name} - ${subject.credits} Credits - Grade: ${subject.grade}",
                        modifier = Modifier.align(Alignment.Start),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collective CGPA Button
        Button(onClick = {
            collectiveCGPA = calculateCollectiveCGPA(semestersList)
        }) {
            Text("Calculate Collective CGPA")
        }

        collectiveCGPA?.let {
            Text(text = "Collective CGPA: $it", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

fun deleteSemesterFromFirestore(userEmail: String, semesterName: String, onSuccess: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("users")
        .document(userEmail)
        .collection("semesters")
        .document(semesterName)
        .delete()
        .addOnSuccessListener {
            println("Semester deleted successfully!")
            onSuccess()
        }
        .addOnFailureListener { e ->
            println("Error deleting semester: $e")
        }
}


fun calculateCGPA(subjects: List<Subject>): Float {
    var totalCredits = 0
    var totalGradePoints = 0.0f
    subjects.forEach {
        totalCredits += it.credits
        totalGradePoints += convertGradeToPoints(it.grade) * it.credits
    }
    return if (totalCredits > 0) totalGradePoints / totalCredits else 0f
}

fun convertGradeToPoints(grade: String): Float {
    return when (grade) {
        "O" -> 10f
        "A+" -> 9f
        "A" -> 8f
        "B+" -> 7f
        "B" -> 6f
        "C" -> 5f
        "F" -> 0f
        else -> 0f
    }
}

fun calculateCollectiveCGPA(semesters: List<Semester>): Float {
    var totalCredits = 0.0 // Declare totalCredits as Double
    var totalGradePoints = 0.0 // Keep totalGradePoints as Double
    semesters.forEach { semester ->
        totalCredits += semester.subjects.sumOf { it.credits.toDouble() } // Convert credits to Double
        totalGradePoints += semester.subjects.sumOf { convertGradeToPoints(it.grade) * it.credits.toDouble() } // Convert credits to Double
    }
    return if (totalCredits > 0) (totalGradePoints / totalCredits).toFloat() else 0f
}


fun saveSemesterToFirestore(userEmail: String, semester: Semester) {
    val firestore = FirebaseFirestore.getInstance()
    val semesterData = hashMapOf(
        "name" to semester.name,
        "subjects" to semester.subjects.map { mapOf("name" to it.name, "credits" to it.credits, "grade" to it.grade) },
        "cgpa" to semester.cgpa
    )
    firestore.collection("users")
        .document(userEmail)
        .collection("semesters")
        .document(semester.name)
        .set(semesterData)
        .addOnSuccessListener {
            println("Semester saved successfully!")
        }
        .addOnFailureListener { e ->
            println("Error saving semester: $e")
        }
}

fun fetchSemestersFromFirestore(userEmail: String, callback: (List<Semester>) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("users")
        .document(userEmail)
        .collection("semesters")
        .get()
        .addOnSuccessListener { result ->
            val semesters = result.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val cgpa = doc.getDouble("cgpa")?.toFloat() ?: 0f
                val subjects = (doc.get("subjects") as? List<Map<String, Any>>)?.mapNotNull { subjectMap ->
                    val subjectName = subjectMap["name"] as? String ?: return@mapNotNull null
                    val credits = (subjectMap["credits"] as? Long)?.toInt() ?: return@mapNotNull null
                    val grade = subjectMap["grade"] as? String ?: return@mapNotNull null
                    Subject(subjectName, credits, grade)
                } ?: return@mapNotNull null
                Semester(name, subjects, cgpa)
            }
            callback(semesters)
        }
        .addOnFailureListener { e ->
            println("Error fetching semesters: $e")
        }
}
