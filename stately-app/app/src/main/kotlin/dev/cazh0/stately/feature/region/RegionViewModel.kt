package dev.cazh0.stately.feature.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.stately.core.result.LoadState
import dev.cazh0.stately.core.result.launchLoad
import dev.cazh0.stately.data.region.Region
import dev.cazh0.stately.data.region.RegionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegionViewModel(
    private val repository: RegionRepository,
    private val regionId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<Region>>(LoadState.Loading)
    val state: StateFlow<LoadState<Region>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchLoad(_state) { repository.load(regionId) }

    companion object {
        fun factory(repository: RegionRepository, regionId: String) = viewModelFactory {
            initializer { RegionViewModel(repository, regionId) }
        }
    }
}
