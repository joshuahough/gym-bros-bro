package com.gymbrosbro.app.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity

// Room relations don't order children, so each wrapper exposes a sorted view.

data class TemplateExerciseWithExercise(
    @Embedded val templateExercise: TemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class TemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(entity = TemplateExerciseEntity::class, parentColumn = "id", entityColumn = "templateId")
    val exercises: List<TemplateExerciseWithExercise>,
) {
    val sortedExercises get() = exercises.sortedBy { it.templateExercise.position }
}

data class SessionExerciseWithSets(
    @Embedded val sessionExercise: SessionExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SetEntryEntity>,
) {
    val sortedSets get() = sets.sortedBy { it.position }
}

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>,
) {
    val sortedExercises get() = exercises.sortedBy { it.sessionExercise.position }
}

/** One past performance of an exercise: the session it was in plus its sets. */
data class ExercisePerformance(
    @Embedded val sessionExercise: SessionExerciseEntity,
    @Relation(parentColumn = "sessionId", entityColumn = "id")
    val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SetEntryEntity>,
) {
    val sortedSets get() = sets.sortedBy { it.position }
}
