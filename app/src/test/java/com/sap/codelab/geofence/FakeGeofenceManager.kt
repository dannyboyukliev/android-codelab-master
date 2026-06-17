package com.sap.codelab.geofence

internal class FakeGeofenceManager : IGeofenceManager {

    val addedGeofences = mutableListOf<Triple<Long, Double, Double>>()
    val removedGeofenceIds = mutableListOf<Long>()

    override suspend fun add(memoId: Long, latitude: Double, longitude: Double) {
        addedGeofences.add(Triple(memoId, latitude, longitude))
    }

    override suspend fun remove(memoId: Long) {
        removedGeofenceIds.add(memoId)
    }
}
