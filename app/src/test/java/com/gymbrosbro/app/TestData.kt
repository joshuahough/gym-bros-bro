package com.gymbrosbro.app

import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.LoadClass
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.model.TrackingType

/** Builders for unit tests; only the fields a test cares about need setting. */
object TestData {
    fun exercise(
        id: String = "ex",
        name: String = "Exercise",
        trackingType: TrackingType = TrackingType.WEIGHT_REPS,
        bodyweightFactor: Double? = null,
        aliases: List<String> = emptyList(),
        muscle: MuscleGroup = MuscleGroup.CHEST,
        equipment: Equipment = Equipment.BARBELL,
        isFavorite: Boolean = false,
    ) = ExerciseEntity(
        id = id,
        name = name,
        aliases = aliases,
        discipline = Discipline.WEIGHTLIFTING,
        trackingType = trackingType,
        equipment = equipment,
        primaryMuscle = muscle,
        secondaryMuscles = emptyList(),
        mechanic = null,
        recommendedLoad = LoadClass.MEDIUM,
        recommendedSets = 3,
        recommendedRepsMin = 8,
        recommendedRepsMax = 12,
        recommendedDurationSec = null,
        recommendedDistanceM = null,
        isUnilateral = false,
        bodyweightFactor = bodyweightFactor,
        weightIncrementKg = null,
        instructions = emptyList(),
        source = ExerciseSource.SEED,
        isFavorite = isFavorite,
    )

    fun set(
        position: Int = 0,
        weightKg: Double? = null,
        reps: Int? = null,
        completed: Boolean = true,
        warmup: Boolean = false,
        addedWeightKg: Double? = null,
        durationSec: Int? = null,
        distanceM: Double? = null,
    ) = SetEntryEntity(
        id = position.toLong() + 1,
        sessionExerciseId = 1,
        position = position,
        isWarmup = warmup,
        isCompleted = completed,
        weightKg = weightKg,
        reps = reps,
        addedWeightKg = addedWeightKg,
        durationSec = durationSec,
        distanceM = distanceM,
    )
}
