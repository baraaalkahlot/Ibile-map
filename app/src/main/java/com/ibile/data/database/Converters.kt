package com.ibile.data.database

import androidx.room.TypeConverter
import java.util.*


object Converters {
    @JvmStatic
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @JvmStatic
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @JvmStatic
    @TypeConverter
    fun fromString(string: String): List<String> = string.split(",")

    @JvmStatic
    @TypeConverter
    fun toString(list: List<String>): String = list.joinToString(separator = ", ")
}
