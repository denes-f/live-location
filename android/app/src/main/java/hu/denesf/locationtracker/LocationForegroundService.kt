package hu.denesf.locationtracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import java.time.Instant
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class LocationForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_START = "hu.denesf.locationtracker.action.START"
        private const val ACTION_STOP = "hu.denesf.locationtracker.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val logTag = "LocationService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildNotification()
                startForeground(NOTIFICATION_ID, notification)

                startLocationUpdates()
            }
            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        // If killed by the system, try to recreate the service later with a null intent
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            Log.w(logTag, "Missing location permission, stopping service.")
            stopSelf()
            return
        }

        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    val loc = result.lastLocation ?: return

                    Log.d(
                        logTag,
                        "Location update: lat=${loc.latitude}, lon=${loc.longitude}, acc=${loc.accuracy}"
                    )
                    serviceScope.launch {
                        try {
                            val settingsRepo = SettingsRepository(this@LocationForegroundService)
                            val settings = settingsRepo.settingsFlow.first()
                            val deviceId = settings.deviceId ?: "android-test-device"

                            val payload = LocationPayload(
                                device_id = deviceId,
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                speed = if (loc.hasSpeed()) loc.speed.toDouble() else null,
                                accuracy = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                                timestamp = Instant.ofEpochMilli(System.currentTimeMillis()).toString()
                            )

                            val batch = LocationBatchPayload(
                                locations = listOf(payload)
                            )

                            ApiClient.api.sendLocation(batch)
                            Log.d(logTag, "Location sent to backend for deviceId=$deviceId")
                        } catch (e: Exception) {
                            Log.e(logTag, "Failed to send location to backend", e)
                        }
                    }
                }
            }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            60_000L // 60 seconds nominal interval
        )
            .setMinUpdateIntervalMillis(10_000L) // fastest ~10 seconds when moving
            .setMinUpdateDistanceMeters(5f)      // ~5m movement threshold
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Location Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows a notification while your location is being tracked."
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)

        val pendingFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            pendingFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Sharing your location with locations.denesf.hu")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}

data class LocationPayload(
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double?,
    val accuracy: Double?,
    val timestamp: String
)

data class LocationBatchPayload(
    val locations: List<LocationPayload>
)

interface LocationApiService {
    @POST("/api/v1/locations")
    suspend fun sendLocation(@Body payload: LocationBatchPayload)
}

object ApiClient {
    val api: LocationApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://locations.denesf.hu")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LocationApiService::class.java)
    }
}