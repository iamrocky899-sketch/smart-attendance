package com.attendancehalim.smartattendance.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.attendancehalim.smartattendance.SmartAttendanceApp
import java.util.concurrent.TimeUnit

class AttendanceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SmartAttendanceApp ?: return Result.failure()
        val attendanceRepo = app.container.attendanceRepository

        return try {
            val syncResult = attendanceRepo.syncPendingAttendance()
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME_ONE_TIME = "attendance_sync_one_time"
        private const val SYNC_WORK_NAME_PERIODIC = "attendance_sync_periodic"

        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
