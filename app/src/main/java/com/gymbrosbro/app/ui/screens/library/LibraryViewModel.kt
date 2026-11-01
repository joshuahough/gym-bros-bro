package com.gymbrosbro.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.ExerciseRepository
import com.gymbrosbro.app.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(repository: ExerciseRepository) : ViewModel() {
    /** null while the first query (or first-launch seeding) is still running. */
    val exercises: StateFlow<List<ExerciseEntity>?> = repository.observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
