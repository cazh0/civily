package dev.cazh0.stately.feature.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cazh0.stately.R
import dev.cazh0.stately.core.graph
import dev.cazh0.stately.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onSignedIn: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graph = LocalContext.current.graph
    val viewModel: SignInViewModel = viewModel(factory = SignInViewModel.factory(graph.authRepository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    var nationName by rememberSaveable { mutableStateOf("") }

    // Why `remember` and not `rememberSaveable`: saveable state is written into the saved
    // instance Bundle, which the system may persist to disk and restore after a reboot. A
    // password must not survive the composition that collected it (spec §3).
    var password by remember { mutableStateOf("") }

    val working = state is SignInState.Working
    val failure = (state as? SignInState.Failed)?.error

    LaunchedEffect(state) {
        val done = state as? SignInState.Done ?: return@LaunchedEffect
        password = ""
        onSignedIn(done.nationId)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_sign_in)) }) },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = nationName,
                onValueChange = {
                    nationName = it
                    viewModel.dismissFailure()
                },
                label = { Text(stringResource(R.string.label_nation_name)) },
                singleLine = true,
                enabled = !working,
                isError = failure != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.dismissFailure()
                },
                label = { Text(stringResource(R.string.label_password)) },
                singleLine = true,
                enabled = !working,
                isError = failure != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (failure != null) {
                Text(
                    text = stringResource(failure.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (working) {
                CircularProgressIndicator(Modifier.size(Dimens.ProgressSize))
            } else {
                Button(
                    onClick = { viewModel.signIn(nationName.trim(), password) },
                    enabled = nationName.isNotBlank() && password.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.action_sign_in))
                }
            }

            Text(
                text = stringResource(R.string.sign_in_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
