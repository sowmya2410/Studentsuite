package com.example.studentsuite



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PomodoroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroTimer()
        }
    }
}

@Composable
fun PomodoroTimer() {
    var isRunning by remember { mutableStateOf(false) }
    var isWorkPhase by remember { mutableStateOf(true) }
    var remainingTime by remember { mutableStateOf(25 * 60) } // Default work duration: 25 minutes
    val workDuration = remember { mutableStateOf(25 * 60) }
    val breakDuration = remember { mutableStateOf(5 * 60) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (remainingTime > 0 && isRunning) {
                delay(1000L)
                remainingTime--
            }
            if (remainingTime == 0 && isRunning) {
                isWorkPhase = !isWorkPhase
                remainingTime = if (isWorkPhase) workDuration.value else breakDuration.value
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isWorkPhase) "Work Time!" else "Break Time!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatTime(remainingTime),
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRunning) {
            Button(onClick = { isRunning = false }) {
                Text("Pause")
            }
        } else {
            Button(onClick = { isRunning = true }) {
                Text("Start")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isRunning = false
                isWorkPhase = true
                remainingTime = workDuration.value
            }
        ) {
            Text("Reset")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Input for work and break durations
        OutlinedTextField(
            value = (workDuration.value / 60).toString(),
            onValueChange = {
                val value = it.toIntOrNull() ?: 25
                workDuration.value = value * 60
                if (isWorkPhase) remainingTime = workDuration.value
            },
            label = { Text("Work Duration (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = (breakDuration.value / 60).toString(),
            onValueChange = {
                val value = it.toIntOrNull() ?: 5
                breakDuration.value = value * 60
                if (!isWorkPhase) remainingTime = breakDuration.value
            },
            label = { Text("Break Duration (minutes)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

@Preview(showBackground = true)
@Composable
fun PomodoroTimerPreview() {
    PomodoroTimer()
}
