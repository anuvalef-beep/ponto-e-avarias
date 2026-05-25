package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PontoDatabase
import com.example.data.model.DamageReport
import com.example.data.model.PontoReminder
import com.example.data.model.WorkDay
import com.example.data.repository.PontoRepository
import com.example.util.PontoReminderHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PontoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PontoDatabase.getDatabase(application)
    private val repository = PontoRepository(database.pontoDao())

    val workDays: StateFlow<List<WorkDay>> = repository.allWorkDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val damageReports: StateFlow<List<DamageReport>> = repository.allDamageReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reminders: StateFlow<List<PontoReminder>> = repository.allReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate default reminders if empty
        viewModelScope.launch {
            val currentList = repository.getAllRemindersList()
            if (currentList.isEmpty()) {
                val defaults = listOf(
                    PontoReminder(label = "Entrada", hour = 8, minute = 0, isEnabled = false),
                    PontoReminder(label = "Pausa", hour = 12, minute = 0, isEnabled = false),
                    PontoReminder(label = "Retorno", hour = 13, minute = 0, isEnabled = false),
                    PontoReminder(label = "Fim", hour = 17, minute = 0, isEnabled = false)
                )
                defaults.forEach { reminder ->
                    val id = repository.insertReminder(reminder)
                    // We don't schedule by default unless enabled, keep it disabled
                }
            }
        }
    }

    // --- WORK DAY ACTIONS ---

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date())
    }

    fun getTodayFormatted(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    fun punchToday(type: String, customPrefix: String = "") {
        val todayStr = getTodayDateString()
        val timeNow = getCurrentTimeFormatted()

        viewModelScope.launch {
            val existing = repository.getWorkDayByDate(todayStr)
            if (existing != null) {
                val updated = when (type) {
                    "entrada" -> existing.copy(entrada = timeNow, isFolga = false, prefix = if (customPrefix.isNotEmpty()) customPrefix else existing.prefix)
                    "pausa" -> existing.copy(pausa = timeNow, isFolga = false, prefix = if (customPrefix.isNotEmpty()) customPrefix else existing.prefix)
                    "retorno" -> existing.copy(retorno = timeNow, isFolga = false, prefix = if (customPrefix.isNotEmpty()) customPrefix else existing.prefix)
                    "fim" -> existing.copy(fim = timeNow, isFolga = false, prefix = if (customPrefix.isNotEmpty()) customPrefix else existing.prefix)
                    "folga" -> existing.copy(isFolga = true, entrada = null, pausa = null, retorno = null, fim = null, prefix = "")
                    else -> existing
                }
                repository.updateWorkDay(updated)
            } else {
                val newRecord = when (type) {
                    "entrada" -> WorkDay(dateString = todayStr, entrada = timeNow, prefix = customPrefix)
                    "pausa" -> WorkDay(dateString = todayStr, pausa = timeNow, prefix = customPrefix)
                    "retorno" -> WorkDay(dateString = todayStr, retorno = timeNow, prefix = customPrefix)
                    "fim" -> WorkDay(dateString = todayStr, fim = timeNow, prefix = customPrefix)
                    "folga" -> WorkDay(dateString = todayStr, isFolga = true)
                    else -> WorkDay(dateString = todayStr)
                }
                repository.insertWorkDay(newRecord)
            }
        }
    }

    fun saveWorkDay(workDay: WorkDay) {
        viewModelScope.launch {
            if (workDay.id == 0) {
                repository.insertWorkDay(workDay)
            } else {
                repository.updateWorkDay(workDay)
            }
        }
    }

    fun deleteWorkDay(workDay: WorkDay) {
        viewModelScope.launch {
            repository.deleteWorkDay(workDay)
        }
    }

    // --- DAMAGE REPORT ACTIONS ---

    fun saveDamageReport(prefix: String, photoPath: String, description: String, dateString: String) {
        viewModelScope.launch {
            val report = DamageReport(
                dateString = dateString,
                prefix = prefix,
                photoPath = photoPath,
                description = description
            )
            repository.insertDamageReport(report)
        }
    }

    fun deleteDamageReport(report: DamageReport) {
        viewModelScope.launch {
            repository.deleteDamageReport(report)
        }
    }

    // --- REMINDER ACTIONS ---

    fun saveReminder(reminder: PontoReminder) {
        viewModelScope.launch {
            val existing = reminder.id != 0
            val id = if (existing) {
                repository.updateReminder(reminder)
                reminder.id
            } else {
                repository.insertReminder(reminder).toInt()
            }
            
            val updatedReminder = reminder.copy(id = id)
            if (updatedReminder.isEnabled) {
                PontoReminderHelper.scheduleReminder(getApplication(), updatedReminder)
            } else {
                PontoReminderHelper.cancelReminder(getApplication(), updatedReminder)
            }
        }
    }

    fun toggleReminder(reminder: PontoReminder, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = isEnabled)
            repository.updateReminder(updated)
            if (isEnabled) {
                PontoReminderHelper.scheduleReminder(getApplication(), updated)
            } else {
                PontoReminderHelper.cancelReminder(getApplication(), updated)
            }
        }
    }

    fun deleteReminder(reminder: PontoReminder) {
        viewModelScope.launch {
            PontoReminderHelper.cancelReminder(getApplication(), reminder)
            repository.deleteReminder(reminder)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("PontoViewModel", "Cleared ViewModel")
    }
}
