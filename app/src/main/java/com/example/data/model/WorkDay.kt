package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // format YYYY-MM-DD
    val prefix: String = "", // Prefix of the minibus for this day
    val entrada: String? = null, // e.g. "08:00"
    val pausa: String? = null,   // e.g. "12:00"
    val retorno: String? = null, // e.g. "13:00"
    val fim: String? = null,     // e.g. "17:00"
    val isFolga: Boolean = false, // True if the user registered a day off
    val notes: String = ""
)
