package com.sap.codelab.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.sap.codelab.notification.NotificationHelper
import com.sap.codelab.repository.IMemoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sap.codelab.BuildConfig
import javax.inject.Inject

private const val TAG = "GeofenceBroadcastReceiver"

@AndroidEntryPoint
internal class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: IMemoRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            if (BuildConfig.DEBUG) Log.e(TAG, "GeofencingEvent error: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val geofence = event.triggeringGeofences?.firstOrNull() ?: return
        val memoId = geofence.requestId.toLongOrNull() ?: return

        if (BuildConfig.DEBUG) Log.d(TAG, "ENTER transition for memo $memoId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val memo = repository.getMemoById(memoId)
                notificationHelper.notify(memo)
            } catch (e: NoSuchElementException) {
                if (BuildConfig.DEBUG) Log.w(TAG, "No memo found for geofence id $memoId")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
