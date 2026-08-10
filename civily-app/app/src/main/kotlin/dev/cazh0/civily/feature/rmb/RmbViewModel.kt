package dev.cazh0.civily.feature.rmb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.civily.core.result.LoadState
import dev.cazh0.civily.core.result.launchLoad
import dev.cazh0.civily.data.rmb.RmbPost
import dev.cazh0.civily.data.rmb.RmbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RmbViewModel(
    private val repository: RmbRepository,
    private val regionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<List<RmbPost>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<RmbPost>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchLoad(_state) { repository.load(regionId) }

    companion object {
        fun factory(repository: RmbRepository, regionId: String) = viewModelFactory {
            initializer { RmbViewModel(repository, regionId) }
        }
    }
}
