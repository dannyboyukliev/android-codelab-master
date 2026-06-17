package com.sap.codelab.di

import android.content.Context
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.sap.codelab.geofence.GeofenceManager
import com.sap.codelab.geofence.IGeofenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceManager(impl: GeofenceManager): IGeofenceManager

    companion object {

        @Provides
        @Singleton
        fun provideGeofencingClient(@ApplicationContext context: Context): GeofencingClient =
            LocationServices.getGeofencingClient(context)
    }
}
