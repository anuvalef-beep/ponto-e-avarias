package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "damage_reports")
data class DamageReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val prefix: String, // Minibus Prefix
    val photoPath: String, // Saved image path/URI
    val description: String, // Description of the damage
    val timestamp: Long = System.currentTimeMillis()
)
