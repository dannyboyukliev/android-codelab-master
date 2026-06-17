package com.sap.codelab.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeofenceManager"
private const val GEOFENCE_RADIUS_METERS = 200f

@Singleton
internal class GeofenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: GeofencingClient
) : IGeofenceManager {

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override suspend fun add(memoId: Long, latitude: Double, longitude: Double) {
        val geofence = Geofence.Builder()
            .setRequestId(memoId.toString())
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            client.addGeofences(request, pendingIntent).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofence for memo $memoId", e)
        }
    }
}
