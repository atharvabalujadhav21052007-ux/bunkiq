package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

object CampusLocationManager {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun calculateDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onLocationReceived: (Location?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onLocationReceived(null)
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        onLocationReceived(loc)
                    } else {
                        // Fallback to last known location
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                            onLocationReceived(lastLoc)
                        }.addOnFailureListener {
                            onLocationReceived(null)
                        }
                    }
                }
                .addOnFailureListener {
                    onLocationReceived(null)
                }
        } catch (e: Exception) {
            onLocationReceived(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun setupSilentHardwareGeofence(
        context: Context,
        config: com.example.model.CampusGeofenceConfig
    ) {
        if (!hasLocationPermission(context)) return

        try {
            val geofencingClient = LocationServices.getGeofencingClient(context)
            val geofence = com.google.android.gms.location.Geofence.Builder()
                .setRequestId("CAMPUS_ATTENDANCE_GEOFENCE")
                .setCircularRegion(config.latitude, config.longitude, config.radiusMeters)
                .setExpirationDuration(com.google.android.gms.location.Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER or
                            com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()

            val geofencingRequest = com.google.android.gms.location.GeofencingRequest.Builder()
                .setInitialTrigger(com.google.android.gms.location.GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            val intent = android.content.Intent(context, CampusGeofenceReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )

            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
        } catch (e: Exception) {
            // Silent fallback
        }
    }
}
