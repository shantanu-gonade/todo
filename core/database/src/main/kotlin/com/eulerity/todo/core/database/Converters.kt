package com.eulerity.todo.core.database

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class Converters {
    @TypeConverter
    fun dateToEpochDay(d: LocalDate?): Long? = d?.toEpochDays()?.toLong()

    @TypeConverter
    fun epochDayToDate(v: Long?): LocalDate? = v?.let { LocalDate.fromEpochDays(it.toInt()) }

    @TypeConverter
    fun instantToMillis(i: Instant?): Long? = i?.toEpochMilliseconds()

    @TypeConverter
    fun millisToInstant(v: Long?): Instant? = v?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun timeToSeconds(t: LocalTime?): Int? = t?.toSecondOfDay()

    @TypeConverter
    fun secondsToTime(v: Int?): LocalTime? = v?.let { LocalTime.fromSecondOfDay(it) }
}
