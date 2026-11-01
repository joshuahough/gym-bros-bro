package com.gymbrosbro.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.LoadClass
import com.gymbrosbro.app.domain.model.Mechanic
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.model.TrackingType
import java.time.Instant

/** SPEC §4.1. Seed rows are read-only except the user flags at the bottom. */
@Entity(
    tableName = "exercise",
    indices = [Index("name"), Index("primaryMuscle")],
)
data class ExerciseEntity(
    /** Stable slug (`barbell-bench-press-medium-grip`); custom ones are `custom-<uuid>`. */
    @PrimaryKey val id: String,
    val name: String,
    val aliases: List<String>,
    val discipline: Discipline,
    val trackingType: TrackingType,
    val equipment: Equipment,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val mechanic: Mechanic?,
    val recommendedLoad: LoadClass,
    val recommendedSets: Int?,
    val recommendedRepsMin: Int?,
    val recommendedRepsMax: Int?,
    val recommendedDurationSec: Int?,
    val recommendedDistanceM: Double?,
    val isUnilateral: Boolean,
    /** Share of bodyweight moved per rep (push-up ≈ 0.65); null = no weight counted. */
    val bodyweightFactor: Double?,
    /** Overrides the equipment's default increment for suggestions. */
    val weightIncrementKg: Double?,
    val instructions: List<String>,
    val source: ExerciseSource,
    // User flags (preserved across seed updates)
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val userNotes: String? = null,
    val archivedAt: Instant? = null,
)
