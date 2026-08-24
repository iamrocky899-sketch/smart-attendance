package com.attendancehalim.smartattendance.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.attendancehalim.smartattendance.core.common.Resource
import com.attendancehalim.smartattendance.domain.model.LocationDetails
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

import kotlinx.coroutines.withTimeoutOrNull

interface LocationClient {
    suspend fun getCurrentLocation(): Resource<LocationDetails>
    fun isLocationEnabled(): Boolean
    fun hasLocationPermission(): Boolean
}

class DefaultLocationClient(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
) : LocationClient {

    override fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override suspend fun getCurrentLocation(): Resource<LocationDetails> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Resource.Error("Location permission is required to mark attendance.")
        }

        if (!isLocationEnabled()) {
            return@withContext Resource.Error("Location is OFF. Please turn on Location to mark attendance.")
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()

            // 1. Attempt fast high accuracy location acquisition (5 second timeout)
            val freshLocation: Location? = try {
                withTimeoutOrNull(5000L) {
                    suspendCancellableCoroutine { continuation ->
                        try {
                            fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                cancellationTokenSource.token
                            ).addOnSuccessListener { loc ->
                                if (continuation.isActive) {
                                    continuation.resume(loc)
                                }
                            }.addOnFailureListener {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        } catch (e: SecurityException) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }

                        continuation.invokeOnCancellation {
                            cancellationTokenSource.cancel()
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }

            // 2. Fallback to cached last known location if fresh acquisition timed out
            val finalLocation: Location? = freshLocation ?: try {
                suspendCancellableCoroutine { continuation ->
                    try {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { loc ->
                                if (continuation.isActive) {
                                    continuation.resume(loc)
                                }
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                    } catch (_: SecurityException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            } catch (_: Exception) {
                null
            }

            if (finalLocation == null) {
                return@withContext Resource.Error("Unable to acquire GPS location. Please check your GPS signal and try again.")
            }

            // 3. Resolve address name with timeout (2 seconds max)
            val addressName = try {
                withTimeoutOrNull(2500L) {
                    resolveAddress(finalLocation.latitude, finalLocation.longitude)
                } ?: "Location Ready"
            } catch (_: Exception) {
                "Location Ready"
            }

            Resource.Success(
                LocationDetails(
                    latitude = finalLocation.latitude,
                    longitude = finalLocation.longitude,
                    accuracy = if (finalLocation.accuracy > 0f) finalLocation.accuracy else 5f,
                    addressName = addressName
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to retrieve location: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    private fun resolveAddress(latitude: Double, longitude: Double): String {
        return try {
            if (!Geocoder.isPresent()) {
                return "Location Ready"
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val subLocality = address.subLocality
                val featureName = address.featureName
                val locality = address.locality
                val subAdminArea = address.subAdminArea
                val adminArea = address.adminArea

                val primary = subLocality ?: (if (featureName != null && !featureName.matches(Regex("^[0-9+]+$"))) featureName else locality)
                val secondary = if (primary != locality) locality ?: subAdminArea else subAdminArea
                val state = adminArea

                val parts = listOfNotNull(primary, secondary, state)
                    .filter { it.isNotBlank() }
                    .distinct()

                if (parts.isNotEmpty()) {
                    parts.joinToString(", ")
                } else {
                    "Location Ready"
                }
            } else {
                "Location Ready"
            }
        } catch (_: Exception) {
            "Location Ready"
        }
    }
}
