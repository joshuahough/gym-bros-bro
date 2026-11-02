package com.gymbrosbro.app.data

import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.avatar.Locker
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.db.entity.EarnedTrophyEntity
import com.gymbrosbro.app.data.db.entity.UnlockedItemEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.progress.Trophies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Trophies and locker unlocks (SPEC §7.9–7.10). Unlike XP they're stored:
 * once earned they stay, whatever happens to the history afterwards.
 */
class AwardsRepository(
    database: GymDatabase,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
) {
    private val dao = database.awardsDao()
    private val mutex = Mutex()

    val trophies: Flow<List<EarnedTrophyEntity>> = dao.observeTrophies()
    val unlocks: Flow<List<UnlockedItemEntity>> = dao.observeUnlocks()
    val unlockedIds: Flow<Set<String>> = unlocks.map { list -> list.mapTo(mutableSetOf()) { it.itemId } }

    /** What the player is wearing, with anything no longer wearable swapped for the default. */
    val avatar: Flow<AvatarConfig> = combine(settings.avatar, unlockedIds, Locker::sanitize)

    suspend fun setAvatar(config: AvatarConfig) = settings.setAvatar(config)

    /**
     * Grants any trophies and unlocks the history now qualifies for. Called
     * after each finished workout (crediting [sessionId]) and at launch.
     */
    suspend fun sync(sessionId: Long?) = mutex.withLock {
        val now = Instant.now()
        val current = progress.current()

        val earned = dao.trophyIds().toSet()
        val newTrophies = Trophies.reached(current.lifetime).filter { it.id !in earned }
        dao.insertTrophies(newTrophies.map { EarnedTrophyEntity(it.id, now, sessionId) })

        val trophyIds = earned + newTrophies.map { it.id }
        val unlocked = dao.unlockedIds().toSet()
        val newItems = Locker.items.filter {
            it.id !in unlocked && !Locker.isUnlocked(it, unlocked) && Locker.qualifies(it, current.xp.level.level, trophyIds)
        }
        dao.insertUnlocks(newItems.map { UnlockedItemEntity(it.id, now, sessionId) })
    }
}
