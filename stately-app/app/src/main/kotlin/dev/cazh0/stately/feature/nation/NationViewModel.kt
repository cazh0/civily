package dev.cazh0.stately.feature.nation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.stately.core.result.LoadState
import dev.cazh0.stately.core.result.launchLoad
import dev.cazh0.stately.data.nation.NationDto
import dev.cazh0.stately.data.nation.NationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the nation screen's state across configuration changes.
 *
 * Why this shape closes the crash class in spec §5: the result of a request lands in a
 * [StateFlow] the ViewModel owns, not in a callback holding a Fragment. There is no
 * `isAdded()` check to forget, because there is nothing here that can outlive its view.
 */
class NationViewModel(
    private val repository: NationRepository,
    private val nationId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<NationDto>>(LoadState.Loading)
    val state: StateFlow<LoadState<NationDto>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchLoad(_state) { repository.load(nationId) }

    companion object {
        fun factory(repository: NationRepository, nationId: String) = viewModelFactory {
            initializer { NationViewModel(repository, nationId) }
        }
    }
}
