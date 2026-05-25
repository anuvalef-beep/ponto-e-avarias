package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DamageReport
import com.example.data.model.PontoReminder
import com.example.data.model.WorkDay

@Database(entities = [WorkDay::class, DamageReport::class, PontoReminder::class], version = 1, exportSchema = false)
abstract class PontoDatabase : RoomDatabase() {
    abstract fun pontoDao(): PontoDao

    companion object {
        @Volatile
        private var INSTANCE: PontoDatabase? = null

        fun getDatabase(context: Context): PontoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PontoDatabase::class.java,
                    "ponto_e_avarias_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
