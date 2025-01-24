package com.example.composetutorial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(onNavigateToConversation: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "This is the Profile Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToConversation) {
            Text(text = "Back to Conversation")
        }
    }
}