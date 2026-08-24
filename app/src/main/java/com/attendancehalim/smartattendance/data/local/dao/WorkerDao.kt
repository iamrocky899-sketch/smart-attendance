package com.attendancehalim.smartattendance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.attendancehalim.smartattendance.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workers: List<WorkerEntity>)

    @Query("SELECT * FROM worker_profiles WHERE employeeId = :employeeId LIMIT 1")
    fun getWorkerById(employeeId: String): Flow<WorkerEntity?>

    @Query("SELECT * FROM worker_profiles WHERE employeeId = :employeeId LIMIT 1")
    suspend fun getWorkerDirect(employeeId: String): WorkerEntity?

    @Query("SELECT * FROM worker_profiles ORDER BY fullName ASC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM worker_profiles ORDER BY employeeId ASC")
    suspend fun getAllWorkersList(): List<WorkerEntity>

    @Query("SELECT COUNT(*) FROM worker_profiles")
    fun getWorkerCount(): Flow<Int>

    @Query("UPDATE worker_profiles SET isActive = :isActive WHERE employeeId = :employeeId")
    suspend fun updateWorkerStatus(employeeId: String, isActive: Boolean)

    @Query("UPDATE worker_profiles SET password = :password WHERE employeeId = :employeeId")
    suspend fun updateWorkerPassword(employeeId: String, password: String)

    @androidx.room.Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("DELETE FROM worker_profiles WHERE employeeId = :employeeId")
    suspend fun deleteWorker(employeeId: String)
}
