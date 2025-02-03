package com.example.composetutorial

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import coil3.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val todoViewModel = ViewModelProvider(this)[TodoViewModel::class.java]
        setContent {
            ComposeTutorialTheme {
                val navController = rememberNavController()
                MyAppNavHost(navController = navController, context = this, todoViewModel)
            }
        }
    }
}

@Composable
fun database(viewModel: TodoViewModel): Todo?{
    val todoList by viewModel.todoList.observeAsState()
    return todoList?.lastOrNull()
}

fun copyImageToAppStorage(uri: Uri, context: Context): String? {
    val fileName = getFileNameFromUri(uri, context) ?: "profile_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)

    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file.absolutePath
}

@SuppressLint("Range")
fun getFileNameFromUri(uri: Uri, context: Context): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }
    return name
}



@Composable
fun ProfileScreen(
    onNavigateToConversation: () -> Unit,
    selectedImagePath: String?,
    onImageSelected: (String) -> Unit, // Save file path instead of URI
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    context: Context,
    viewModel: TodoViewModel
) {
    var editedName by remember { mutableStateOf(profileName) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val storedPath = copyImageToAppStorage(it, context) // Save in storage
                storedPath?.let { path -> onImageSelected(path) } // Update state
            }
        }
    )

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imagePainter = selectedImagePath?.let {
            rememberAsyncImagePainter(File(it)) // Load image from storage
        } ?: painterResource(R.drawable.noprofile)

        Image(
            painter = imagePainter,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = editedName,
            onValueChange = { editedName = it },
            label = { Text("Profile Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (editedName.isNotBlank()) {
                onProfileNameChange(editedName)
                viewModel.addTodo(editedName, selectedImagePath)
            }
        }) {
            Text(text = "Save Profile Info")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Text(text = "Pick Profile Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToConversation) {
            Text(text = "Back to Conversation")
        }
    }
}

@Composable
fun Conversation(
    messages: List<Message>,
    profileImagePath: String?,
    profileName: String,
    onNavigateToProfile: () -> Unit
) {
    Column {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                MessageCard(message, profileImagePath, profileName)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToProfile, modifier = Modifier.padding(16.dp)) {
            Text(text = "Go to Profile")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Profile Name: $profileName", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MessageCard(msg: Message, profileImagePath: String?, profileName: String) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        val imagePainter = profileImagePath?.let {
            rememberAsyncImagePainter(File(it)) // Load from file path
        } ?: painterResource(R.drawable.noprofile)

        Image(
            painter = imagePainter,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        var isExpanded by remember { mutableStateOf(false) }
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = profileName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MyAppNavHost(navController: NavHostController, context: Context, viewModel: TodoViewModel) {
    val item = database(viewModel)
    var profilesOldName by remember { mutableStateOf("NoProfileName") }
    var profilePicture by remember { mutableStateOf("") }
    var isThereProfileName by remember { mutableStateOf(true) }
    var isThereProfilePicture by remember { mutableStateOf(true) }
    item?.let { // This ensures item is not null
        if (it.title != null) {
            profilesOldName = it.title
        } else {
            isThereProfileName = false
        }

        if (it.picture != null) {
            profilePicture = it.picture!!
        } else {
            isThereProfilePicture = false
        }
    }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }
    if (isThereProfilePicture) {
        selectedImagePath = profilePicture
    }

    var profileName by remember { mutableStateOf("NoProfileName") }
    if (isThereProfileName) {
        profileName = profilesOldName
    }

    NavHost(navController = navController, startDestination = "conversation") {
        composable("conversation") {
            Conversation(
                messages = SampleData.conversationSample,
                profileImagePath = selectedImagePath,
                profileName = profileName,
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateToConversation = { navController.navigate("conversation") },
                selectedImagePath = selectedImagePath,
                onImageSelected = { newPath -> selectedImagePath = newPath },
                profileName = profileName,
                onProfileNameChange = { newName -> profileName = newName },
                context = context,
                viewModel = viewModel()
            )
        }
    }
}

data class Message(val author: String, val body: String)

/*@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Preview
@Composable
fun PreviewMessageCard() {
    ComposeTutorialTheme {
        Surface {
            MessageCard(
                msg = Message("NoProfileName", "Hey, take a look at Jetpack Compose, it's great!"),
                profileImagePath = null, // Correct parameter name
                profileName = "NoProfileName" // Mock profile name
            )
        }
    }
}*/

