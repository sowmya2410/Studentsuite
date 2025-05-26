package com.example.studentsuite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.*

data class Document(
    val subject: String = "",
    val fileName: String = "",
    val fileUri: String = "",  // Store URI as a String
    val documentId: String? = null // Firestore document ID
)

class DocumentManagementActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocumentManagementScreen(auth, firestore)
        }
    }
}

// Composable function for Document Management Screen
@Composable
fun DocumentManagementScreen(auth: FirebaseAuth, firestore: FirebaseFirestore) {
    val context = LocalContext.current

    var subject by remember { mutableStateOf(TextFieldValue()) }
    val documents = remember { mutableStateListOf<Document>() }

    // Get the current user's email
    val userEmail = UserSession.userEmail ?: return // Exit if the user is not logged in

    // Launcher to pick a document
    val pickDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val fileName = getFileName(context, it) ?: "Unknown File"
                val document = Document(subject.text, fileName, it.toString()) // Store URI as String
                saveDocumentToFirebase(userEmail, document, firestore, context)

                // Save document to Firebase
                Toast.makeText(context, "Picked: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Fetch documents from Firestore
    LaunchedEffect(userEmail) {
        firestore.collection("users")
            .document(userEmail)
            .collection("documents")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading documents: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                snapshot?.documents?.let {
                    documents.clear()
                    documents.addAll(it.mapNotNull { doc ->
                        doc.toObject<Document>()?.copy(documentId = doc.id)
                    })
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Input field to enter subject name
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Button to pick document
        Button(onClick = { pickDocument.launch("application/pdf") }) {
            Text("Pick Document (PDF/PPT)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grouping documents by subject
        val groupedDocuments = documents.groupBy { it.subject }

        LazyColumn {
            groupedDocuments.forEach { (subjectName, docs) ->
                item {
                    Text(
                        text = "Documents for $subjectName",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(docs) { document ->
                    DocumentItem(
                        document,
                        onClick = {
                            // Open the file when clicked
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(document.fileUri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(intent)
                        },
                        onDelete = {
                            // Delete the document from Firestore
                            document.documentId?.let { deleteDocumentFromFirebase(userEmail, it, firestore, context) }
                            documents.remove(document)
                        }
                    )
                }
            }
        }
    }
}

// Function to save document to Firestore

// Function to save document to Firestore
fun saveDocumentToFirebase(userEmail: String, document: Document, firestore: FirebaseFirestore, context: android.content.Context) {
    firestore.collection("users")
        .document(userEmail)
        .collection("documents")
        .add(document)
        .addOnSuccessListener {
            Toast.makeText(context, "Document uploaded", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error uploading document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

// Function to delete document from Firestore
fun deleteDocumentFromFirebase(userEmail: String, documentId: String, firestore: FirebaseFirestore, context: android.content.Context) {
    firestore.collection("users")
        .document(userEmail)
        .collection("documents")
        .document(documentId)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error deleting document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

// Composable function to display a document item in the list
@Composable
fun DocumentItem(
    document: Document,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = document.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Delete Button
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = "Delete", color = Color.White)
            }
        }
    }
}

// Function to extract file name from URI
fun getFileName(context: android.content.Context, uri: Uri): String? {
    var fileName: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex != -1) {
                fileName = it.getString(columnIndex)
            }
        }
    }
    return fileName
}

// Preview function for Document Management screen
@Preview(showBackground = true)
@Composable
fun DocumentManagementScreenPreview() {
    DocumentManagementScreen(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
}
