package com.gymbrosbro.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import com.gymbrosbro.app.domain.library.LibraryFilter
import com.gymbrosbro.app.domain.library.LibraryQuery
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.MuscleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class LibraryUiState(
    val query: LibraryQuery,
    val results: List<ExerciseEntity>,
    val totalCount: Int,
)

class LibraryViewModel(repository: ExerciseRepository) : ViewModel() {
    private val query = MutableStateFlow(LibraryQuery())

    /** Picker mode only: chosen exercise ids, in the order they were tapped. */
    private val _selected = MutableStateFlow<List<String>>(emptyList())
    val selected: StateFlow<List<String>> = _selected.asStateFlow()

    /** null while the first query (or first-launch seeding) is still running. */
    val state: StateFlow<LibraryUiState?> = combine(repository.observeLibrary(), query) { all, q ->
        LibraryUiState(q, LibraryFilter.apply(all, q), all.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setText(text: String) = query.update { it.copy(text = text) }
    fun setMuscle(muscle: MuscleGroup?) = query.update { it.copy(muscle = muscle) }
    fun setEquipment(equipment: Equipment?) = query.update { it.copy(equipment = equipment) }
    fun toggleFavorites() = query.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    fun toggleSelected(id: String) = _selected.update { if (id in it) it - id else it + id }
}
