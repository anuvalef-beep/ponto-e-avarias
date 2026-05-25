package com.example.data.repository

import com.example.data.local.PontoDao
import com.example.data.model.DamageReport
import com.example.data.model.PontoReminder
import com.example.data.model.WorkDay
import kotlinx.coroutines.flow.Flow

class PontoRepository(private val pontoDao: PontoDao) {

    // Work Day logic
    val allWorkDays: Flow<List<WorkDay>> = pontoDao.getAllWorkDaysFlow()

    suspend fun getWorkDayByDate(date: String): WorkDay? {
        return pontoDao.getWorkDayByDate(date)
    }

    suspend fun insertWorkDay(workDay: WorkDay): Long {
        return pontoDao.insertWorkDay(workDay)
    }

    suspend fun updateWorkDay(workDay: WorkDay) {
        pontoDao.updateWorkDay(workDay)
    }

    suspend fun deleteWorkDay(workDay: WorkDay) {
        pontoDao.deleteWorkDay(workDay)
    }

    suspend fun deleteWorkDayById(id: Int) {
        pontoDao.deleteWorkDayById(id)
    }

    // Damage Reports logic
    val allDamageReports: Flow<List<DamageReport>> = pontoDao.getAllDamageReportsFlow()

    suspend fun insertDamageReport(damageReport: DamageReport): Long {
        return pontoDao.insertDamageReport(damageReport)
    }

    suspend fun updateDamageReport(damageReport: DamageReport) {
        pontoDao.updateDamageReport(damageReport)
    }

    suspend fun deleteDamageReport(damageReport: DamageReport) {
        pontoDao.deleteDamageReport(damageReport)
    }

    suspend fun deleteDamageReportById(id: Int) {
        pontoDao.deleteDamageReportById(id)
    }

    // Reminder logic
    val allReminders: Flow<List<PontoReminder>> = pontoDao.getAllRemindersFlow()

    suspend fun getAllRemindersList(): List<PontoReminder> {
        return pontoDao.getAllRemindersList()
    }

    suspend fun getReminderById(id: Int): PontoReminder? {
        return pontoDao.getReminderById(id)
    }

    suspend fun insertReminder(reminder: PontoReminder): Long {
        return pontoDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: PontoReminder) {
        pontoDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: PontoReminder) {
        pontoDao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: Int) {
        pontoDao.deleteReminderById(id)
    }
}
