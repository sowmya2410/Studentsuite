package com.example.studentsuite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter

// Data model for FlashCard
data class FlashCard(
    val question: String,
    val answer: String
)

class FlashCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlashCardScreen()
        }
    }
}

// Composable function for FlashCard Screen
@Composable
fun FlashCardScreen() {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var flashCardsList by remember { mutableStateOf(listOf<FlashCard>()) } // Immutable list of flashcards

    // Firebase Firestore instance
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Get the current logged-in user email (ensure user is authenticated)
    val emailId = UserSession.userEmail ?: ""

    // Fetch flashcards from Firestore
    LaunchedEffect(emailId) {
        if (emailId.isNotEmpty()) {
            fetchFlashCardsFromFirestore(firestore, emailId) { fetchedCards ->
                flashCardsList = fetchedCards // Update the state with the fetched cards
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Input fields to add a flashcard
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Question") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text("Answer") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Button to add the flashcard
        Button(onClick = {
            if (question.isNotEmpty() && answer.isNotEmpty()) {
                val flashCard = FlashCard(question, answer)
                flashCardsList = flashCardsList + flashCard // Add new flashcard to the list
                saveFlashCardToFirestore(firestore, emailId, flashCard) // Save to Firestore
                question = "" // Clear input fields
                answer = ""
            }
        }) {
            Text(text = "Add FlashCard")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Display the list of flashcards
        LazyColumn {
            items(flashCardsList) { flashCard ->
                FlashCardView(flashCard)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Composable function for FlashCard with flip functionality
@Composable
fun FlashCardView(flashCard: FlashCard) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .graphicsLayer(
                rotationY = rotation,
                cameraDistance = 12f * density()
            )
            .clickable { isFlipped = !isFlipped }
            .padding(8.dp)
    ) {
        if (rotation <= 90f) {
            // Front side (Question)
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = flashCard.question,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Back side (Answer)
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = flashCard.answer,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer(rotationY = 180f)
                )
            }
        }
    }
}

// Utility to get screen density
@Composable
fun density(): Float {
    return LocalDensity.current.density
}

// Function to fetch flashcards from Firestore
fun fetchFlashCardsFromFirestore(firestore: FirebaseFirestore, emailId: String, onFetchComplete: (List<FlashCard>) -> Unit) {
    firestore.collection("users")
        .document(emailId) // Use emailId as the user document ID
        .collection("flashcards") // Use a subcollection for flashcards
        .get()
        .addOnSuccessListener { snapshot ->
            val flashCardsList = snapshot.documents.mapNotNull { document ->
                val question = document.getString("question")
                val answer = document.getString("answer")
                if (question != null && answer != null) {
                    FlashCard(question, answer)
                } else {
                    null
                }
            }
            onFetchComplete(flashCardsList)
        }
        .addOnFailureListener {
            onFetchComplete(emptyList())
        }
}

// Function to save flashcards to Firestore
fun saveFlashCardToFirestore(firestore: FirebaseFirestore, emailId: String, flashCard: FlashCard) {
    val flashCardData = mapOf(
        "question" to flashCard.question,
        "answer" to flashCard.answer
    )

    firestore.collection("users")
        .document(emailId) // Use emailId as the user document ID
        .collection("flashcards") // Use a subcollection for flashcards
        .add(flashCardData) // Save the new card as a document in the subcollection
        .addOnSuccessListener {
            // Flashcard saved successfully
        }
        .addOnFailureListener {
            // Handle error saving flashcard
        }
}

// Preview function
@Preview(showBackground = true)
@Composable
fun FlashCardScreenPreview() {
    FlashCardScreen()
}
