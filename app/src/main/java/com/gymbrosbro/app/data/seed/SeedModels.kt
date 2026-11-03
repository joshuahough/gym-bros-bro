package com.gymbrosbro.app.data.seed

import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.LoadClass
import com.gymbrosbro.app.domain.model.Mechanic
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.model.TrackingType
import kotlinx.serialization.Serializable

/** Shape of `assets/exercises.json`, produced by `scripts/build_seed/build_seed.py`. */
@Serializable
data class SeedFile(
    val version: Int,
    val source: String,
    val exercises: List<SeedExercise>,
)

@Serializable
data class SeedExercise(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val discipline: Discipline,
    val trackingType: TrackingType,
    val equipment: Equipment,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val mechanic: Mechanic? = null,
    val recommendedLoad: LoadClass,
    val recommendedSets: Int? = null,
    val recommendedRepsMin: Int? = null,
    val recommendedRepsMax: Int? = null,
    val recommendedDurationSec: Int? = null,
    val recommendedDistanceM: Double? = null,
    val isUnilateral: Boolean = false,
    val bodyweightFactor: Double? = null,
    val weightIncrementKg: Double? = null,
    val instructions: List<String> = emptyList(),
    val alternateId: String? = null,
) {
    fun toEntity() = ExerciseEntity(
        id = id,
        name = name,
        aliases = aliases,
        discipline = discipline,
        trackingType = trackingType,
        equipment = equipment,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles,
        mechanic = mechanic,
        recommendedLoad = recommendedLoad,
        recommendedSets = recommendedSets,
        recommendedRepsMin = recommendedRepsMin,
        recommendedRepsMax = recommendedRepsMax,
        recommendedDurationSec = recommendedDurationSec,
        recommendedDistanceM = recommendedDistanceM,
        isUnilateral = isUnilateral,
        bodyweightFactor = bodyweightFactor,
        weightIncrementKg = weightIncrementKg,
        instructions = instructions,
        source = ExerciseSource.SEED,
        alternateId = alternateId,
    )
}
