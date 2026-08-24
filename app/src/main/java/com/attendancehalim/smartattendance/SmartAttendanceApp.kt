package com.attendancehalim.smartattendance

import android.app.Application
import com.attendancehalim.smartattendance.data.di.AppContainer
import com.attendancehalim.smartattendance.data.di.DefaultAppContainer

class SmartAttendanceApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
