package com.example.lab08

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

class RoomResearch {
    class DateConverters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }
    }

    @Database(entities = [Task::class], version = 1)
    @TypeConverters(DateConverters::class)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun taskDao(): TaskDao
    }

}