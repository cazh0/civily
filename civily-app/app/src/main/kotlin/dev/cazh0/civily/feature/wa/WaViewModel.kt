package dev.cazh0.civily.feature.wa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.civily.core.result.LoadState
import dev.cazh0.civily.core.result.launchLoad
import dev.cazh0.civily.data.wa.Assembly
import dev.cazh0.civily.data.wa.AssemblyRepository
import dev.cazh0.civily.data.wa.Council
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One instance per chamber, keyed by council.
 *
 * Why not one ViewModel holding both: each chamber is a separate request with its own
 * failure. Sharing state would mean a Security Council timeout blanking a General Assembly
 * resolution that loaded perfectly well.
 */
class WaViewModel(
    private val repository: AssemblyRepository,
    private val council: Council,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<Assembly>>(LoadState.Loading)
    val state: StateFlow<LoadState<Assembly>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchLoad(_state) { repository.load(council) }

    companion object {
        fun factory(repository: AssemblyRepository, council: Council) = viewModelFactory {
            initializer { WaViewModel(repository, council) }
        }
    }
}
