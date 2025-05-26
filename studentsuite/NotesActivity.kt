package com.example.studentsuite

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Data model for Notes
data class Note(
    val title: String,
    val description: String,
    val emailId: String,
    val noteId: String? = null // Add noteId for Firestore document reference
)

class NotesActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesScreen(auth, firestore)
        }
    }
}

// Composable function for Notes screen
@Composable
fun NotesScreen(auth: FirebaseAuth, firestore: FirebaseFirestore) {
    var noteTitle by remember { mutableStateOf("") }
    var noteDescription by remember { mutableStateOf("") }
    var notesList by remember { mutableStateOf<List<Note>>(emptyList()) }

    // Use Firebase Authentication to get the current user email
    val currentUser = auth.currentUser
    val userEmail = UserSession.userEmail ?: "" // Correctly fetching user email

    // Load notes from Firestore
    LaunchedEffect(userEmail) {
        if (userEmail.isNotEmpty()) {
            fetchNotes(firestore, userEmail) { notes ->
                notesList = notes
            }
        }
    }

    val context = LocalContext.current // Get context for Toast

    // Add new note function
    fun addNote() {
        if (noteTitle.isNotEmpty() && noteDescription.isNotEmpty() && userEmail.isNotEmpty()) {
            val newNote = Note(noteTitle, noteDescription, userEmail)

            // Add note to Firestore
            firestore.collection("notes")
                .add(newNote)
                .addOnSuccessListener {
                    noteTitle = ""
                    noteDescription = ""
                    fetchNotes(firestore, userEmail) { notes ->
                        notesList = notes // Update notes list
                    }
                    Toast.makeText(context, "Note added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to add note.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Delete note function
    fun deleteNote(noteId: String) {
        firestore.collection("notes")
            .document(noteId) // Use noteId for Firestore document reference
            .delete()
            .addOnSuccessListener {
                // Update the list by removing the deleted note
                notesList = notesList.filter { it.noteId != noteId }
                Toast.makeText(context, "Note deleted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete note.", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Input fields for creating a new note
        OutlinedTextField(
            value = noteTitle,
            onValueChange = { noteTitle = it },
            label = { Text("Note Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // The note description field with more space
        OutlinedTextField(
            value = noteDescription,
            onValueChange = { noteDescription = it },
            label = { Text("Note Description") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)  // Give more vertical space for description
                .padding(bottom = 16.dp),
            maxLines = 10,  // Allow multiple lines of text
            minLines = 5     // Start with a reasonable height for the field
        )

        // Button to add a new note
        Button(onClick = { addNote() }) {
            Text(text = "Add Note")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the list of notes
        Text(text = "Notes", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // LazyColumn to display each note
        LazyColumn {
            items(notesList) { note ->
                NoteItem(note, onDelete = {
                    // Call deleteNote function when delete button is clicked
                    deleteNote(note.noteId ?: "")
                })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Fetch notes from Firestore
fun fetchNotes(firestore: FirebaseFirestore, userEmail: String, onNotesFetched: (List<Note>) -> Unit) {
    firestore.collection("notes")
        .whereEqualTo("emailId", userEmail)
        .get()
        .addOnSuccessListener { result ->
            val notes = result.map { document ->
                val title = document.getString("title") ?: ""
                val description = document.getString("description") ?: ""
                val noteId = document.id // Get Firestore document ID
                Note(title, description, userEmail, noteId) // Add noteId to Note object
            }
            onNotesFetched(notes)
        }
}

// Composable function for displaying each note item
@Composable
fun NoteItem(note: Note, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Delete button
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Delete", color = Color.White)
            }
        }
    }
}

// Preview function
@Preview(showBackground = true)
@Composable
fun NotesScreenPreview() {
    // Replace with actual authentication and firestore instance in real use case
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    NotesScreen(auth, firestore)
}
