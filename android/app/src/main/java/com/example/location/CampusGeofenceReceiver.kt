package com.example.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AttendanceRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * 100% Silent Background Geofence Receiver.
 * Executes purely in the background without raising any notifications, status bar alerts, or sounds.
 * Automatically marks ongoing lecture as PRESENT when entering campus boundary,
 * or ABSENT (Auto-Bunk) when exiting campus boundary.
 */
class CampusGeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Log.w("GeofenceReceiver", "Geofence event error code: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        val repository = AttendanceRepository(context)

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d("GeofenceReceiver", "Silent transition ENTER triggered -> auto marking present")
                repository.processGeofenceTransition(isInside = true)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d("GeofenceReceiver", "Silent transition EXIT triggered -> auto bunk triggered")
                repository.processGeofenceTransition(isInside = false)
            }
        }
    }
}
