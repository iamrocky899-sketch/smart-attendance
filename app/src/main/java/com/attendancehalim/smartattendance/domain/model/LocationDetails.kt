package com.attendancehalim.smartattendance.domain.model

data class LocationDetails(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val addressName: String
)
