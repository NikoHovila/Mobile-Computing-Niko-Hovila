package com.example.composetutorial

import android.app.*
import android.content.Intent
import android.hardware.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class GyroscopeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    override fun onCreate() {
        super.onCreate()

        // Get SensorManager and Gyroscope
        sensorManager = getSystemService(SensorManager::class.java)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Register Gyroscope Listener
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Start Foreground Notification (Required for Background Services)
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "gyroscope_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gyroscope Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Gyroscope Service Running")
            .setContentText("Detecting rotations in the background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val rotationThreshold = 2.0f  // Adjust sensitivity
            val rotationX = it.values[0]
            val rotationY = it.values[1]
            val rotationZ = it.values[2]

            if (Math.abs(rotationX) > rotationThreshold ||
                Math.abs(rotationY) > rotationThreshold ||
                Math.abs(rotationZ) > rotationThreshold) {
                sendRotationNotification()
            }
        }
    }

    private fun sendRotationNotification() {
        val channelId = "rotation_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Gyroscope Notifications", NotificationManager.IMPORTANCE_HIGH
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}