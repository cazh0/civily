package dev.cazh0.civily.feature.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.civily.core.result.LoadState
import dev.cazh0.civily.core.result.launchLoad
import dev.cazh0.civily.data.issues.IssueBadge
import dev.cazh0.civily.data.issues.IssuesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the Issues button on the front screen knows.
 *
 * Separate from [IssuesViewModel] rather than shared with it because they are scoped to
 * different destinations and want different things: this one wants two numbers cheaply and
 * often, that one wants every issue in full, once. Sharing would mean the front screen paying
 * for the issues screen's payload on every resume.
 *
 * Why it does not load in `init`: the button is worth exactly one thing, an honest count, and
 * a count that was true when the app started is not one. The screen drives it from its own
 * lifecycle instead — see `LookupScreen` — which is also what refreshes it the moment the
 * reader comes back from answering something.
 *
 * [LoadState.Ready.refreshing] is why a reload never blanks the badge: the digit that is on
 * screen stays there until a truer one arrives.
 */
class IssueBadgeViewModel(private val repository: IssuesRepository) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<IssueBadge>>(LoadState.Loading)
    val state: StateFlow<LoadState<IssueBadge>> = _state.asStateFlow()

    private var inFlight: Job? = null

    /**
     * Why one at a time: the button has two reasons to ask — the reader arriving on the screen,
     * and the wait for the next issue running out — and on the resume after a wait that expired
     * in the background both arrive in the same frame. Left ungated that is two requests for the
     * same two numbers, every time.
     */
    fun refresh() {
        if (inFlight?.isActive == true) return
        inFlight = launchLoad(_state) { repository.badge() }
    }

    companion object {
        fun factory(repository: IssuesRepository) = viewModelFactory {
            initializer { IssueBadgeViewModel(repository) }
        }
    }
}
