package dev.cazh0.civily.core.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * What a screen that reads one thing can be.
 *
 * Why shared rather than one sealed type per feature: every read-only screen has exactly
 * these three states, and twenty private copies of them is twenty places to get the same
 * `when` wrong. A screen that gains a fourth state — mid-vote, mid-post — declares its own
 * type at that point; it does not bend this one.
 */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    /**
     * @param refreshing true while a reload is in flight over content already on screen.
     *   Kept on [Ready] rather than as a fourth state because the content is still valid and
     *   must keep rendering — replacing it with a spinner on every pull-to-refresh is the
     *   flicker that makes an app feel cheap.
     */
    data class Ready<out T>(val value: T, val refreshing: Boolean = false) : LoadState<T>

    data class Failed(val error: CivilyError) : LoadState<Nothing>
}

/**
 * Runs [load] and drives [into] through Loading to Ready or Failed.
 *
 * Why an extension and not a shared base ViewModel: the loading mechanic is identical across
 * features, but the ViewModels are not — nation will grow issue answering, region will grow
 * message posting. Sharing the mechanic keeps the duplication out without welding the
 * features together (spec §2 R2a).
 *
 * Returns the [Job] so a ViewModel with more than one reason to reload can tell whether it is
 * already doing so. The state cannot answer that: it is written from inside the coroutine, so
 * two callers in one frame both read the state from before either of them started.
 */
fun <T> ViewModel.launchLoad(
    into: MutableStateFlow<LoadState<T>>,
    load: suspend () -> Outcome<T>,
): Job =
    viewModelScope.launch {
        // Why the first load and a refresh differ: a first load has nothing to show, so the
        // spinner is the screen. A refresh has content, so the spinner belongs beside it.
        into.value = when (val current = into.value) {
            is LoadState.Ready -> current.copy(refreshing = true)
            else -> LoadState.Loading
        }
        into.value = load().fold(
            onSuccess = { value -> LoadState.Ready(value) },
            onFailure = { error -> LoadState.Failed(error) },
        )
    }
