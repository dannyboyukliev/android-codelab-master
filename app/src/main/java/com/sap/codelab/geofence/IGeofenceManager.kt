package com.sap.codelab.geofence

internal interface IGeofenceManager {
    suspend fun add(memoId: Long, latitude: Double, longitude: Double)
    suspend fun remove(memoId: Long)
}
