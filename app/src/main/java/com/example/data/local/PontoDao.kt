package com.example.data.local

import androidx.room.*
import com.example.data.model.DamageReport
import com.example.data.model.PontoReminder
import com.example.data.model.WorkDay
import kotlinx.coroutines.flow.Flow

@Dao
interface PontoDao {
    // Work Days
    @Query("SELECT * FROM work_days ORDER BY dateString DESC")
    fun getAllWorkDaysFlow(): Flow<List<WorkDay>>

    @Query("SELECT * FROM work_days WHERE dateString = :date LIMIT 1")
    suspend fun getWorkDayByDate(date: String): WorkDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDay): Long

    @Update
    suspend fun updateWorkDay(workDay: WorkDay)

    @Delete
    suspend fun deleteWorkDay(workDay: WorkDay)

    @Query("DELETE FROM work_days WHERE id = :id")
    suspend fun deleteWorkDayById(id: Int)

    // Damage Reports
    @Query("SELECT * FROM damage_reports ORDER BY timestamp DESC")
    fun getAllDamageReportsFlow(): Flow<List<DamageReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamageReport(damageReport: DamageReport): Long

    @Update
    suspend fun updateDamageReport(damageReport: DamageReport)

    @Delete
    suspend fun deleteDamageReport(damageReport: DamageReport)

    @Query("DELETE FROM damage_reports WHERE id = :id")
    suspend fun deleteDamageReportById(id: Int)

    // Ponto Reminders
    @Query("SELECT * FROM ponto_reminders ORDER BY hour ASC, minute ASC")
    fun getAllRemindersFlow(): Flow<List<PontoReminder>>

    @Query("SELECT * FROM ponto_reminders")
    suspend fun getAllRemindersList(): List<PontoReminder>

    @Query("SELECT * FROM ponto_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Int): PontoReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: PontoReminder): Long

    @Update
    suspend fun updateReminder(reminder: PontoReminder)

    @Delete
    suspend fun deleteReminder(reminder: PontoReminder)

    @Query("DELETE FROM ponto_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
}
