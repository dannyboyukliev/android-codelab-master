package com.sap.codelab.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sap.codelab.repository.IMemoRepository
import com.sap.codelab.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootReceiver"

@AndroidEntryPoint
internal class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: IMemoRepository
    @Inject lateinit var geofenceManager: IGeofenceManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.getOpen()
                    .filter { it.reminderLatitude != null && it.reminderLongitude != null }
                    .forEach { memo ->
                        geofenceManager.add(memo.id, memo.reminderLatitude!!, memo.reminderLongitude!!)
                    }
                if (BuildConfig.DEBUG) Log.d(TAG, "Geofences re-registered after boot")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
