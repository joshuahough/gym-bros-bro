package com.gymbrosbro.app.ui

import androidx.compose.ui.graphics.Color
import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.LoadClass
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.model.TrackingType
import com.gymbrosbro.app.ui.theme.GbbColors

val TrackingType.label: String
    get() = when (this) {
        TrackingType.WEIGHT_REPS -> "Weight × Reps"
        TrackingType.BODYWEIGHT_REPS -> "Bodyweight Reps"
        TrackingType.DURATION -> "Time"
        TrackingType.DISTANCE_DURATION -> "Distance + Time"
        TrackingType.WEIGHT_DISTANCE -> "Weight × Distance"
        TrackingType.REPS -> "Reps Only"
    }

/** One word for the TIME / REPS switch on exercises that can be logged either way. */
val TrackingType.modeLabel: String
    get() = when (this) {
        TrackingType.DURATION -> "TIME"
        TrackingType.REPS, TrackingType.BODYWEIGHT_REPS -> "REPS"
        TrackingType.WEIGHT_REPS -> "WEIGHT"
        TrackingType.DISTANCE_DURATION, TrackingType.WEIGHT_DISTANCE -> "DISTANCE"
    }

val Discipline.label: String
    get() = when (this) {
        Discipline.WEIGHTLIFTING -> "Weightlifting"
        Discipline.CALISTHENICS -> "Calisthenics"
        Discipline.CARDIO -> "Cardio"
        Discipline.MOBILITY -> "Mobility"
        Discipline.PLYOMETRIC -> "Plyometric"
    }

val LoadClass.label: String
    get() = when (this) {
        LoadClass.LIGHT -> "Light"
        LoadClass.MEDIUM -> "Medium"
        LoadClass.HEAVY -> "Heavy"
    }

val LoadClass.color: Color
    get() = when (this) {
        LoadClass.LIGHT -> GbbColors.Green
        LoadClass.MEDIUM -> GbbColors.Gold
        LoadClass.HEAVY -> GbbColors.Red
    }

val MuscleGroup.label: String
    get() = when (this) {
        MuscleGroup.CHEST -> "Chest"
        MuscleGroup.UPPER_BACK -> "Upper Back"
        MuscleGroup.LATS -> "Lats"
        MuscleGroup.LOWER_BACK -> "Lower Back"
        MuscleGroup.SHOULDERS -> "Shoulders"
        MuscleGroup.BICEPS -> "Biceps"
        MuscleGroup.TRICEPS -> "Triceps"
        MuscleGroup.FOREARMS -> "Forearms"
        MuscleGroup.CORE -> "Core"
        MuscleGroup.GLUTES -> "Glutes"
        MuscleGroup.QUADS -> "Quads"
        MuscleGroup.HAMSTRINGS -> "Hamstrings"
        MuscleGroup.CALVES -> "Calves"
        MuscleGroup.FULL_BODY -> "Full Body"
        MuscleGroup.CARDIO -> "Cardio"
    }

/** Icon tint per muscle group, so the library reads at a glance. */
val MuscleGroup.color: Color
    get() = when (this) {
        MuscleGroup.CHEST -> GbbColors.Red
        MuscleGroup.UPPER_BACK -> GbbColors.Blue
        MuscleGroup.LATS -> GbbColors.Cyan
        MuscleGroup.LOWER_BACK -> GbbColors.Purple
        MuscleGroup.SHOULDERS -> GbbColors.Orange
        MuscleGroup.BICEPS -> GbbColors.Gold
        MuscleGroup.TRICEPS -> GbbColors.Pink
        MuscleGroup.FOREARMS -> GbbColors.Brown
        MuscleGroup.CORE -> GbbColors.Green
        MuscleGroup.GLUTES -> GbbColors.Pink
        MuscleGroup.QUADS -> GbbColors.Forest
        MuscleGroup.HAMSTRINGS -> GbbColors.Purple
        MuscleGroup.CALVES -> GbbColors.Cyan
        MuscleGroup.FULL_BODY -> GbbColors.Chalk
        MuscleGroup.CARDIO -> GbbColors.Red
    }

val Equipment.label: String
    get() = when (this) {
        Equipment.BARBELL -> "Barbell"
        Equipment.DUMBBELL -> "Dumbbell"
        Equipment.KETTLEBELL -> "Kettlebell"
        Equipment.MACHINE -> "Machine"
        Equipment.CABLE -> "Cable"
        Equipment.BAND -> "Band"
        Equipment.BODYWEIGHT -> "Bodyweight"
        Equipment.CARDIO_MACHINE -> "Cardio Machine"
        Equipment.OTHER -> "Other"
        Equipment.NONE -> "No Equipment"
    }
