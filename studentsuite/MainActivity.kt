package com.example.studentsuite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studentsuite.ui.theme.StudentsuiteTheme
import com.google.firebase.auth.FirebaseAuth


import com.example.studentsuite.User




class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Check if the user is logged in onActivityCreate
        val currentUser = auth.currentUser

        // Set up the UI based on the login state
        setContent {
            StudentsuiteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var isLoggedIn by remember { mutableStateOf(currentUser != null) }
                    var currentUserState by remember { mutableStateOf<User?>(currentUser?.let {
                        User(it.displayName ?: "Unknown", "", it.email ?: "", "")
                    }) }

                    // List of registered users (mock data for this example)
                    val usersList = remember { mutableStateListOf<User>() }

                    // Display the login screen or welcome page
                    if (isLoggedIn) {
                        WelcomePage(
                            name = currentUserState?.name ?: "Student",
                            onLogoutClick = {
                                auth.signOut()
                                isLoggedIn = false
                                currentUserState = null
                            },

                            onToDoListClick = {
                                startActivity(Intent(this@MainActivity, ToDoListActivity::class.java))
                            },
                            onGradeSheetClick = {
                                startActivity(Intent(this@MainActivity, GradeSheetActivity::class.java))
                            },
                            onAttendanceTrackerClick = {
                                startActivity(Intent(this@MainActivity, AttendanceActivity::class.java))
                            },
                            onPomodoroClick = {
                                startActivity(Intent(this@MainActivity, PomodoroActivity::class.java))
                            },
                            onAssignmentTrackerClick = {
                                startActivity(Intent(this@MainActivity, AssignmentTrackerActivity::class.java))
                            },
                            onEventNewsClick = {
                                startActivity(Intent(this@MainActivity, EventNewsActivity::class.java))
                            },
                            onFlashcardClick = {
                                startActivity(Intent(this@MainActivity, FlashCardActivity::class.java))
                            },
                            onDocumentManagementClick = {
                                startActivity(Intent(this@MainActivity, DocumentManagementActivity::class.java))
                            },
                            onNotesClick = {
                                startActivity(Intent(this@MainActivity, NotesActivity::class.java))
                            },
                            onTimetableClick = {
                                startActivity(Intent(this@MainActivity, TimetableActivity::class.java))
                            }
                        )
                    } else {
                        Greeting(
                            name = "Welcome to Student Suite!",
                            modifier = Modifier.padding(innerPadding),
                            onLoginClick = { isLoggedIn = true },
                            onSignupClick = { /* Navigate to Signup */ },
                            usersList = usersList,
                            onUserLogin = { user ->
                                currentUserState = user
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    usersList: List<User>,
    onUserLogin: (User) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignupVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = name, modifier = Modifier.padding(bottom = 8.dp))

        if (isSignupVisible) {
            // Show Signup UI
            SignupPage(usersList as MutableList<User>, onSignupSuccess = { newUser ->
                isSignupVisible = false
                onUserLogin(newUser) // Set the logged-in user
            })
        } else {
            // Login UI
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.let {
                                // Store email in the global object
                                UserSession.userEmail = it.email
                                onUserLogin(User(user.displayName ?: "Unknown", "", user.email ?: "", password))
                            }
                        } else {
                            errorMessage = task.exception?.message
                        }
                    }
            }) {
                Text(text = "Login")
            }


            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { isSignupVisible = true }) {
                Text(text = "Signup")
            }
        }
    }
}









@Composable
fun WelcomePage(
    name: String,
    onLogoutClick: () -> Unit,
    onToDoListClick: () -> Unit,
    onGradeSheetClick: () -> Unit,
    onAttendanceTrackerClick: () -> Unit,
    onPomodoroClick: () -> Unit,
    onAssignmentTrackerClick: () -> Unit,
    onEventNewsClick: () -> Unit, // New parameter for Event News
    onFlashcardClick: () -> Unit, // New parameter for Flashcards
    onDocumentManagementClick: () -> Unit, // New parameter for Document Management
    onNotesClick: () -> Unit, // New parameter for Notes
    onTimetableClick: () -> Unit // New parameter for Timetable Activity
) {
    val auth = FirebaseAuth.getInstance()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Hello Student!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onLogoutClick) {
            auth.signOut()
            Text(text = "Logout")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onToDoListClick) {
            Text(text = "To-Do List")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onGradeSheetClick) {
            Text(text = "Grade Sheet")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onAttendanceTrackerClick) {
            Text(text = "Attendance Tracker")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPomodoroClick) {
            Text(text = "Pomodoro Timer")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onAssignmentTrackerClick) {
            Text(text = "Assignment Tracker")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onEventNewsClick) { // New button for Event News
            Text(text = "Campus Event News")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onFlashcardClick) { // New button for Flashcards
            Text(text = "Flashcards")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onDocumentManagementClick) { // New button for Document Management
            Text(text = "Document Management")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNotesClick) { // New button for Notes
            Text(text = "Notes")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onTimetableClick) { // New button for Timetable Activity
            Text(text = "Timetable")
        }
    }
}







@Composable
fun SignupPage(usersList: MutableList<User>, onSignupSuccess: (User) -> Unit) {
    var studentName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create a New Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Student Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rollNumber,
            onValueChange = { rollNumber = it },
            label = { Text("Roll Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Firebase sign up
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            // Store email in the global object
                            UserSession.userEmail = it.email
                            val newUser = User(studentName, rollNumber, email, password)
                            usersList.add(newUser)
                            onSignupSuccess(newUser) // Pass the new user to the callback
                        }
                    }
                }
        }) {
            Text(text = "Signup")
        }

    }
}





@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StudentsuiteTheme {
        Greeting(
            name = "Welcome to Student Suite!",
            onLoginClick = {},
            onSignupClick = {},
            usersList = listOf(),
            onUserLogin = {}
        )
    }
}

