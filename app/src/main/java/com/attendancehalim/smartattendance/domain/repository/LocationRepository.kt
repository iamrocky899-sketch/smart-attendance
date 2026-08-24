package com.attendancehalim.smartattendance.domain.repository

import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.LocationDetails

interface LocationRepository {
    suspend fun getCurrentLocation(): Resource<LocationDetails>
    fun isLocationEnabled(): Boolean
    fun hasLocationPermission(): Boolean
}
