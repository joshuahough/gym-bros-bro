@file:UseSerializers(InstantSerializer::class, LocalDateSerializer::class)

package com.gymbrosbro.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gymbrosbro.app.data.backup.InstantSerializer
import com.gymbrosbro.app.data.backup.LocalDateSerializer
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

// SPEC §4.5: trophies and unlocks are permanent once earned, even if the
// workouts that earned them are later edited or deleted.

@Serializable
@Entity(tableName = "earned_trophy")
data class EarnedTrophyEntity(
    @PrimaryKey val trophyId: String,
    val earnedAt: Instant,
    /** The workout that earned it, for the finish screen; null when backfilled. */
    val sessionId: Long?,
)

@Serializable
@Entity(tableName = "unlocked_item")
data class UnlockedItemEntity(
    @PrimaryKey val itemId: String,
    val unlockedAt: Instant,
    val sessionId: Long?,
)
