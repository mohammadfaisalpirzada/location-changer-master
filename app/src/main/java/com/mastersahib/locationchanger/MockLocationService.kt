package com.mastersahib.locationchanger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import java.util.Timer
import kotlin.concurrent.timerTask

class MockLocationService : Service() {
    private lateinit var locationManager: LocationManager
    private var timer: Timer? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var providerAdded = false

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        latitude = intent?.getDoubleExtra(EXTRA_LATITUDE, Double.NaN) ?: Double.NaN
        longitude = intent?.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN) ?: Double.NaN
        if (latitude.isNaN() || longitude.isNaN()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())

        return try {
            configureProvider()
            publishLocation()
            timer?.cancel()
            timer = Timer("mock-location", true).apply {
                scheduleAtFixedRate(timerTask { publishLocation() }, UPDATE_INTERVAL, UPDATE_INTERVAL)
            }
            isRunning = true
            START_STICKY
        } catch (_: SecurityException) {
            stopSelf()
            START_NOT_STICKY
        } catch (_: IllegalArgumentException) {
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun configureProvider() {
        if (providerAdded) return
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            // The provider was not registered by this app yet.
        }
        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER,
            false,
            false,
            false,
            false,
            true,
            true,
            true,
            Criteria.POWER_LOW,
            Criteria.ACCURACY_FINE
        )
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        providerAdded = true
    }

    private fun publishLocation() {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = this@MockLocationService.latitude
            longitude = this@MockLocationService.longitude
            accuracy = 1f
            altitude = 0.0
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            speed = 0f
            bearing = 0f
        }
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_location)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text, latitude, longitude))
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .addAction(
            R.drawable.ic_stop,
            getString(R.string.stop),
            PendingIntent.getService(
                this,
                1,
                Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        if (providerAdded) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
            } catch (_: Exception) {
                // Android may already have removed the provider.
            }
        }
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.mastersahib.locationchanger.START"
        const val ACTION_STOP = "com.mastersahib.locationchanger.STOP"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        private const val CHANNEL_ID = "mock_location"
        private const val NOTIFICATION_ID = 101
        private const val UPDATE_INTERVAL = 1_000L

        @Volatile
        var isRunning = false
            private set
    }
}
