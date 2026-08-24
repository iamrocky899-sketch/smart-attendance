package com.attendancehalim.smartattendance.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendancehalim.smartattendance.data.local.dao.AttendanceDao
import com.attendancehalim.smartattendance.data.local.dao.WorkerDao
import com.attendancehalim.smartattendance.data.local.entity.AttendanceEntity
import com.attendancehalim.smartattendance.data.local.entity.WorkerEntity

@Database(
    entities = [
        AttendanceEntity::class,
        WorkerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartAttendanceDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao
    abstract fun workerDao(): WorkerDao

    companion object {
        private const val DATABASE_NAME = "smart_attendance.db"

        @Volatile
        private var INSTANCE: SmartAttendanceDatabase? = null

        fun getInstance(context: Context): SmartAttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartAttendanceDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
