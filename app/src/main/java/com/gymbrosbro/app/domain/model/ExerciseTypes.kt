package com.gymbrosbro.app.domain.model

// Enum names are persisted (Room stores the name, seed JSON uses the name),
// so never rename an entry — add new ones instead.

enum class Discipline { WEIGHTLIFTING, CALISTHENICS, CARDIO, MOBILITY, PLYOMETRIC }

/** Decides which inputs a set shows and how it feeds the stats (SPEC §4.2). */
enum class TrackingType {
    /** weight × reps */
    WEIGHT_REPS,

    /** reps + optional added weight (negative = assisted); volume uses bodyweight × factor */
    BODYWEIGHT_REPS,

    /** seconds, optionally with added weight */
    DURATION,

    /** distance + seconds (runs, rows, swims) */
    DISTANCE_DURATION,

    /** weight + distance (carries, sleds) */
    WEIGHT_DISTANCE,
}

enum class Equipment {
    BARBELL, DUMBBELL, KETTLEBELL, MACHINE, CABLE, BAND, BODYWEIGHT, CARDIO_MACHINE, OTHER, NONE
}

enum class MuscleGroup {
    CHEST, UPPER_BACK, LATS, LOWER_BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    CORE, GLUTES, QUADS, HAMSTRINGS, CALVES, FULL_BODY, CARDIO
}

enum class Mechanic { COMPOUND, ISOLATION }

/** The exercise's typical load class. Advisory only; never restricts what you log. */
enum class LoadClass { LIGHT, MEDIUM, HEAVY }

enum class ExerciseSource { SEED, CUSTOM }

enum class SessionStatus { IN_PROGRESS, COMPLETED }
