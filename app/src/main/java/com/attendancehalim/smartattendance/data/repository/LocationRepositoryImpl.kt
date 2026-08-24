package com.attendancehalim.smartattendance.data.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.data.location.LocationClient
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import com.attendancehalim.smartattendance.domain.repository.LocationRepository

class LocationRepositoryImpl(
    private val locationClient: LocationClient
) : LocationRepository {

    override suspend fun getCurrentLocation(): Resource<LocationDetails> {
        return locationClient.getCurrentLocation()
    }

    override fun isLocationEnabled(): Boolean {
        return locationClient.isLocationEnabled()
    }

    override fun hasLocationPermission(): Boolean {
        return locationClient.hasLocationPermission()
    }
}
