package com.attendancehalim.smartattendance.data.di

import android.content.Context
import com.attendancehalim.smartattendance.core.config.AppConfig
import com.attendancehalim.smartattendance.core.network.ConnectivityNetworkMonitor
import com.attendancehalim.smartattendance.core.network.NetworkMonitor
import com.attendancehalim.smartattendance.data.local.database.SmartAttendanceDatabase
import com.attendancehalim.smartattendance.data.local.session.SessionManager
import com.attendancehalim.smartattendance.data.location.DefaultLocationClient
import com.attendancehalim.smartattendance.data.location.LocationClient
import com.attendancehalim.smartattendance.data.remote.api.AppsScriptApiService
import com.attendancehalim.smartattendance.data.repository.AttendanceRepositoryImpl
import com.attendancehalim.smartattendance.data.repository.AuthRepositoryImpl
import com.attendancehalim.smartattendance.data.repository.LocationRepositoryImpl
import com.attendancehalim.smartattendance.data.repository.SessionRepositoryImpl
import com.attendancehalim.smartattendance.data.repository.WorkerRepositoryImpl
import com.attendancehalim.smartattendance.domain.repository.AttendanceRepository
import com.attendancehalim.smartattendance.domain.repository.AuthRepository
import com.attendancehalim.smartattendance.domain.repository.LocationRepository
import com.attendancehalim.smartattendance.domain.repository.SessionRepository
import com.attendancehalim.smartattendance.domain.repository.WorkerRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: SmartAttendanceDatabase
    val sessionManager: SessionManager
    val networkMonitor: NetworkMonitor
    val locationClient: LocationClient
    val apiService: AppsScriptApiService
    val sessionRepository: SessionRepository
    val authRepository: AuthRepository
    val attendanceRepository: AttendanceRepository
    val workerRepository: WorkerRepository
    val locationRepository: LocationRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: SmartAttendanceDatabase by lazy {
        SmartAttendanceDatabase.getInstance(context)
    }

    override val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        ConnectivityNetworkMonitor(context)
    }

    override val locationClient: LocationClient by lazy {
        DefaultLocationClient(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = com.attendancehalim.smartattendance.core.network.SanitizedLoggingInterceptor()
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    override val apiService: AppsScriptApiService by lazy {
        retrofit.create(AppsScriptApiService::class.java)
    }

    override val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(sessionManager)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(sessionManager, database.workerDao(), apiService)
    }

    override val workerRepository: WorkerRepository by lazy {
        WorkerRepositoryImpl(database.workerDao(), apiService, sessionManager)
    }

    override val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(
            context = context,
            attendanceDao = database.attendanceDao(),
            apiService = apiService,
            networkMonitor = networkMonitor,
            sessionManager = sessionManager
        )
    }

    override val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(locationClient)
    }
}
