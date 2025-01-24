package com.example.composetutorial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import android.content.res.Configuration
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeTutorialTheme {
                val navController = rememberNavController()
                MyAppNavHost(navController = navController)
            }
        }
    }
}

@Composable
fun MyAppNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = "conversation",
    ) {
        // Conversation Screen
        composable("conversation") {
            Conversation(
                messages = SampleData.conversationSample,
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        // Ensure only one instance of Profile screen exists
                        popUpTo("conversation") {
                            inclusive = false // Keep Conversation in the stack
                        }
                    }
                }
            )
        }

        // Profile Screen
        composable("profile") {
            ProfileScreen(
                onNavigateToConversation = {
                    navController.navigate("conversation") {
                        // Clear Profile from the stack and ensure only one instance of Conversation exists
                        popUpTo("conversation") {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

data class Message(val author: String, val body: String)

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
    ComposeTutorialTheme {
        Surface {
            MessageCard(
                msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
            )
        }
    }
}