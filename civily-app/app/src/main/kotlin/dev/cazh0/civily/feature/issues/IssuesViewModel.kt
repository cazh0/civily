package dev.cazh0.civily.feature.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.civily.core.result.LoadState
import dev.cazh0.civily.core.result.CivilyError
import dev.cazh0.civily.core.result.fold
import dev.cazh0.civily.core.result.launchLoad
import dev.cazh0.civily.data.issues.Issue
import dev.cazh0.civily.data.issues.IssueResult
import dev.cazh0.civily.data.issues.IssuesPage
import dev.cazh0.civily.data.issues.IssuesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Where an answer has got to.
 *
 * Answering is the app's first irreversible action, so it needs states the list never did:
 * one that disables the options while the request is in flight, and one that reports which
 * side of the irreversible boundary failed.
 */
sealed interface AnswerState {
    data object Idle : AnswerState
    data object Sending : AnswerState
    data class Failed(val error: CivilyError) : AnswerState

    /** Carries the consequence, because that is the part the player actually wanted. */
    data class Done(val result: IssueResult) : AnswerState
}

class IssuesViewModel(private val repository: IssuesRepository) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<IssuesPage>>(LoadState.Loading)
    val state: StateFlow<LoadState<IssuesPage>> = _state.asStateFlow()

    /**
     * Why the detail screen reads from here instead of fetching: the list already holds every
     * issue in full. Re-requesting one to open it would spend a request to learn nothing new,
     * and would show a spinner over data the app is already holding.
     */
    fun issue(issueId: Int): Issue? =
        (_state.value as? LoadState.Ready)?.value?.issues?.firstOrNull { it.id == issueId }

    private val _answer = MutableStateFlow<AnswerState>(AnswerState.Idle)
    val answer: StateFlow<AnswerState> = _answer.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchLoad(_state) { repository.load() }

    fun answer(issueId: Int, optionId: Int) {
        if (_answer.value == AnswerState.Sending) return

        viewModelScope.launch {
            _answer.value = AnswerState.Sending
            _answer.value = repository.answer(issueId, optionId).fold(
                onSuccess = { result ->
                    // The issue is gone from the nation now, so the list is stale the moment
                    // this succeeds.
                    refresh()
                    AnswerState.Done(result)
                },
                onFailure = { error ->
                    refresh()
                    AnswerState.Failed(error)
                },
            )
        }
    }

    fun dismissAnswerState() {
        _answer.value = AnswerState.Idle
    }

    companion object {
        fun factory(repository: IssuesRepository) = viewModelFactory {
            initializer { IssuesViewModel(repository) }
        }
    }
}
