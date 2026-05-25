package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ponto_reminders")
data class PontoReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String, // "Entrada", "Pausa", "Retorno", "Fim"
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true
) {
    val timeFormatted: String
        get() = String.format("%02d:%02d", hour, minute)
}
