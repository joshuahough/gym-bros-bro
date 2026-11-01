package com.gymbrosbro.app.data.db

import androidx.room.TypeConverter
import com.gymbrosbro.app.domain.model.MuscleGroup
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun muscleListToJson(value: List<MuscleGroup>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToMuscleList(value: String): List<MuscleGroup> = Json.decodeFromString(value)
}
