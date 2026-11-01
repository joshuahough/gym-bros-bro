package com.gymbrosbro.app.domain.library

import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.MuscleGroup

data class LibraryQuery(
    val text: String = "",
    val muscle: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val favoritesOnly: Boolean = false,
)

object LibraryFilter {
    /**
     * Every search word must appear in the name or an alias. With a search,
     * exact matches rank first, then prefix matches, then the rest by name.
     */
    fun apply(all: List<ExerciseEntity>, query: LibraryQuery): List<ExerciseEntity> {
        val phrase = query.text.trim().lowercase()
        val words = phrase.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val matches = all.filter { e ->
            (query.muscle == null || e.primaryMuscle == query.muscle) &&
                (query.equipment == null || e.equipment == query.equipment) &&
                (!query.favoritesOnly || e.isFavorite) &&
                words.all { w -> e.name.lowercase().contains(w) || e.aliases.any { it.lowercase().contains(w) } }
        }
        if (words.isEmpty()) return matches
        return matches.sortedWith(compareBy({ rank(it, phrase) }, { it.name.lowercase() }))
    }

    private fun rank(e: ExerciseEntity, phrase: String): Int {
        val names = listOf(e.name.lowercase()) + e.aliases.map { it.lowercase() }
        return when {
            names.any { it == phrase } -> 0
            names.any { it.startsWith(phrase) } -> 1
            else -> 2
        }
    }
}
