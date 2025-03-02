package com.example.composetutorial

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import android.net.Uri
import android.os.Build
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
import android.Manifest
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date


class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val todoViewModel = ViewModelProvider(this)[TodoViewModel::class.java]
        val lifecycleObserver = AppLifecycleObserver(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        val serviceIntent = Intent(this, GyroscopeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        setContent {
            ComposeTutorialTheme {
                val navController = rememberNavController()
                MyAppNavHost(navController = navController, context = this, todoViewModel)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val rotationThreshold = 2.0f
            val rotationX = it.values[0]
            val rotationY = it.values[1]
            val rotationZ = it.values[2]

            if (Math.abs(rotationX) > rotationThreshold || Math.abs(rotationY) > rotationThreshold || Math.abs(rotationZ) > rotationThreshold) {
                sendRotationNotification()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    @SuppressLint("MissingPermission")
    private fun sendRotationNotification() {
        val channelId = "rotation_channel"

        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gyroscope Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nice rotation")
            .setContentText("You rotated your phone")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
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
    onNavigateToCamera: () -> Unit,
    onNavigateToCapturedPhotos: () -> Unit,
    selectedImagePath: String?,
    onImageSelected: (String) -> Unit,
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
                val storedPath = copyImageToAppStorage(it, context)
                storedPath?.let { path -> onImageSelected(path) }
            }
        }
    )
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imagePainter = selectedImagePath?.let {
            rememberAsyncImagePainter(File(it))
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
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Text(text = "Pick Profile Photo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToCapturedPhotos) {
            Text(text = "Pick from Captured Photos")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToCamera) {
            Text(text = "Open Camera")
        }
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
    val context = LocalContext.current

    // Track whether notification permission is granted.
    var notificationsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsGranted = true
            permissionDenied = false
            Toast.makeText(context, "ALLOWED", Toast.LENGTH_SHORT).show()
            sendNotification(context)
        } else {
            notificationsGranted = false
            permissionDenied = true
            Toast.makeText(context, "DENIED", Toast.LENGTH_SHORT).show()
        }
    }
    Column {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                MessageCard(message, profileImagePath, profileName)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToProfile,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Go to Profile")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (!notificationsGranted) {
            if (permissionDenied) {
                Text(
                    "Allowing notifications helps you stay updated with important messages and conversations.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Request Notification Permission")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MessageCard(msg: Message, profileImagePath: String?, profileName: String) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        val imagePainter = profileImagePath?.let {
            rememberAsyncImagePainter(File(it))
        } ?: painterResource(R.drawable.noprofile)

        Image(
            painter = imagePainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
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



class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        NotificationHelper(applicationContext).sendNotification()
        return Result.success()
    }
}

class AppLifecycleObserver(private val context: Context) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        scheduleNotification()
    }

    private fun scheduleNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

class NotificationHelper(private val context: Context) {

    fun sendNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "exit_channel",
                "Exit Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "exit_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("I miss you")
            .setContentText("Please come again")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }
}

@SuppressLint("MissingPermission")
fun sendNotification(context: Context) {
    val channelId = "default_channel"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "General Notifications"
        val descriptionText = "Notification Channel for General Updates"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Notifications Enabled")
        .setContentText("Now on you will receive notifications")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.notify(3, notification)
}

@Composable
fun MyAppNavHost(navController: NavHostController, context: Context, viewModel: TodoViewModel) {
    val item = profileInfo(viewModel)
    var profilesOldName by remember { mutableStateOf("NoProfileName") }
    var profilePicture by remember { mutableStateOf("") }
    var isThereProfileName by remember { mutableStateOf(true) }
    var isThereProfilePicture by remember { mutableStateOf(true) }
    item?.let {
        if (it.title.isNotEmpty()) {
            profilesOldName = it.title
        } else {
            isThereProfileName = false
        }
        if (!it.picture.isNullOrEmpty()) {
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
                onNavigateToCamera = { navController.navigate("camera") },
                onNavigateToCapturedPhotos = { navController.navigate("capturedPhotos") },
                selectedImagePath = selectedImagePath,
                onImageSelected = { newPath -> selectedImagePath = newPath },
                profileName = profileName,
                onProfileNameChange = { newName -> profileName = newName },
                context = context,
                viewModel = viewModel
            )
        }
        composable("camera") {
            CameraScreen(navBack = { navController.popBackStack() })
        }
        composable("capturedPhotos") {
            CapturedPhotosGallery(
                viewModel = viewModel,
                onPhotoSelected = { path ->
                    selectedImagePath = path
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}


@Composable
fun profileInfo(viewModel: TodoViewModel): Todo? {
    val todoList by viewModel.todoList.observeAsState(emptyList())
    // Return the last Todo that is not a captured photo.
    return todoList.filter { it.title != "Captured Photo" }.lastOrNull()
}

data class Message(val author: String, val body: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navBack: () -> Unit) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var flashVisible by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        permissionDenied = !granted
    }

    LaunchedEffect(key1 = true) {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (cameraPermissionGranted) {
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState()
        val controller = remember {
            LifecycleCameraController(context).apply {
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
                )
            }
        }

        val todoViewModel: TodoViewModel = viewModel()

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 0.dp,
            sheetContent = {
                CapturedPhotosBar(viewModel = todoViewModel, onPhotoSelected = {})
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CameraPreview(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
                if (flashVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                IconButton(
                    onClick = navBack,
                    modifier = Modifier.offset(16.dp, 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(
                        onClick = { scope.launch { scaffoldState.bottomSheetState.expand() } }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Open gallery"
                        )
                    }
                    IconButton(
                        onClick = {
                            takePhoto(
                                controller = controller,
                                onPhotoTaken = { capturedBitmap ->
                                    // Trigger flash effect.
                                    flashVisible = true
                                    scope.launch {
                                        delay(100L)
                                        flashVisible = false
                                    }
                                    onPhotoTaken(capturedBitmap, context)
                                },
                                context = context
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo"
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (permissionDenied) {
                Text(
                    "Granting camera access allows you to update your profile picture more easily by using our in-app camera.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Grant Camera Permission")
            }
            Button(
                onClick = navBack,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Back")
            }
        }
    }
}

private fun onPhotoTaken(
    capturedBitmap: Bitmap,
    context: Context
) {
    val photoPath = saveBitmapToFile(capturedBitmap, context)
    Thread {
        MainApplication.todoDatabase.getTodoDao().addTodo(
            Todo(title = "Captured Photo", picture = photoPath, createdAt = Date())
        )
    }.start()
}

private fun takePhoto(
    controller: LifecycleCameraController,
    onPhotoTaken: (Bitmap) -> Unit,
    context: Context
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                val matrix = Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    image.toBitmap(),
                    0,
                    0,
                    image.width,
                    image.height,
                    matrix,
                    true
                )
                onPhotoTaken(rotatedBitmap)
                image.close()
            }
            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("Camera", "Couldn't take photo: ", exception)
            }
        }
    )
}


fun saveBitmapToFile(bitmap: Bitmap, context: Context): String {
    val fileName = "photo_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    return file.absolutePath
}

@Composable
fun CapturedPhotosGallery(
    viewModel: TodoViewModel,
    onPhotoSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val todos by viewModel.todoList.observeAsState(emptyList())
    val capturedPhotos = todos.filter { it.title == "Captured Photo" && it.picture != null }

    if (capturedPhotos.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No captured photos available")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) {
                Text("Back")
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(capturedPhotos) { todo ->
                    Image(
                        painter = rememberAsyncImagePainter(File(todo.picture!!)),
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPhotoSelected(todo.picture!!) }
                    )
                }
            }
        }
    }
}

@Composable
fun CapturedPhotosBar(
    viewModel: TodoViewModel,
    onPhotoSelected: (String) -> Unit
) {
    val todos by viewModel.todoList.observeAsState(emptyList())
    val capturedPhotos = todos.filter { it.title == "Captured Photo" && !it.picture.isNullOrEmpty() }

    if (capturedPhotos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No photos taken yet")
        }
    } else {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(capturedPhotos) { todo ->
                Image(
                    painter = rememberAsyncImagePainter(File(todo.picture!!)),
                    contentDescription = "Captured Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPhotoSelected(todo.picture!!) }
                )
            }
        }
    }
}
