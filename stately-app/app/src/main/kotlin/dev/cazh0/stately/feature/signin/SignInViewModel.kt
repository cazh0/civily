package dev.cazh0.stately.feature.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.cazh0.stately.core.result.StatelyError
import dev.cazh0.stately.core.result.fold
import dev.cazh0.stately.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Why this screen declares its own state instead of reusing `LoadState`: it has a fourth
 * state. A sign-in form starts idle, with nothing loading and nothing failed, and `LoadState`
 * has no way to say that without pretending a spinner belongs on an empty form.
 */
sealed interface SignInState {
    data object Idle : SignInState
    data object Working : SignInState
    data class Failed(val error: StatelyError) : SignInState
    data class Done(val nationId: String) : SignInState
}

class SignInViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun signIn(nationName: String, password: String) {
        if (_state.value == SignInState.Working) return

        viewModelScope.launch {
            _state.value = SignInState.Working
            // Why the password is a parameter and never a field: a ViewModel outlives the
            // composition, and a password held there outlives the screen the user typed it on.
            _state.value = auth.signIn(nationName, password).fold(
                onSuccess = { session -> SignInState.Done(session.nationId) },
                onFailure = { error -> SignInState.Failed(error) },
            )
        }
    }

    /** Clears a failure so the form is usable again after the user edits what they typed. */
    fun dismissFailure() {
        if (_state.value is SignInState.Failed) _state.value = SignInState.Idle
    }

    companion object {
        fun factory(auth: AuthRepository) = viewModelFactory {
            initializer { SignInViewModel(auth) }
        }
    }
}
