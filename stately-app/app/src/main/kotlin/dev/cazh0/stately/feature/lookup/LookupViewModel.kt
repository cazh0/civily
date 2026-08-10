package dev.cazh0.stately.feature.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.stately.core.result.StatelyError
import dev.cazh0.stately.core.result.fold
import dev.cazh0.stately.core.text.NsId
import dev.cazh0.stately.data.nation.NationDto
import dev.cazh0.stately.data.nation.NationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Search has four outcomes, and "not found" is not a failure.
 *
 * Why [Missing] is its own state rather than `Failed(NotFound)`: a nation that does not exist
 * is a normal, expected answer to a search. Rendering it with the same red warning as a
 * network outage tells the user something is broken when nothing is — which is exactly what
 * happened when a lookup for a misspelled name dead-ended on a full-screen error.
 */
sealed interface LookupState {
    data object Idle : LookupState
    data object Searching : LookupState
    data class Found(val nationId: String, val nation: NationDto) : LookupState
    data class Missing(val typedName: String) : LookupState
    data class Failed(val error: StatelyError) : LookupState
}

class LookupViewModel(private val repository: NationRepository) : ViewModel() {

    private val _state = MutableStateFlow<LookupState>(LookupState.Idle)
    val state: StateFlow<LookupState> = _state.asStateFlow()

    fun search(typedName: String) {
        val trimmed = typedName.trim()
        val nationId = NsId.fromName(trimmed)
        if (nationId.isEmpty() || _state.value == LookupState.Searching) return

        viewModelScope.launch {
            _state.value = LookupState.Searching
            _state.value = repository.loadPreview(nationId).fold(
                onSuccess = { nation -> LookupState.Found(nationId, nation) },
                onFailure = { error ->
                    if (error == StatelyError.NotFound) {
                        LookupState.Missing(trimmed)
                    } else {
                        LookupState.Failed(error)
                    }
                },
            )
        }
    }

    /** Called as the user edits, so a stale result never sits under a changed query. */
    fun clear() {
        if (_state.value != LookupState.Idle) _state.value = LookupState.Idle
    }

    companion object {
        fun factory(repository: NationRepository) = viewModelFactory {
            initializer { LookupViewModel(repository) }
        }
    }
}
