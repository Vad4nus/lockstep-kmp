package io.github.vad4nus.sample.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vad4nus.sample.ProfileAction
import io.github.vad4nus.sample.ProfileScreenState
import io.github.vad4nus.sample.ProfileViewModel

@Composable
fun ProfileScreen() {
    val viewModel = remember { ProfileViewModel() }
    DisposableEffect(viewModel) { onDispose { viewModel.clear() } }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var backTaps by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                ProfileAction.NavigateBack -> backTaps++
                is ProfileAction.ShowToast -> toast = action.text
            }
        }
    }

    Column(Modifier.safeDrawingPadding().padding(16.dp)) {
        OutlinedTextField(value = state.name, onValueChange = viewModel::onNameChanged, label = { Text("Name") })
        Text("Age: ${state.age ?: "-"}")
        Text("City: ${state.address?.label() ?: "-"}")
        when (val screen = state.screen) {
            ProfileScreenState.Idle -> Text("Idle")
            ProfileScreenState.Loading -> CircularProgressIndicator()
            is ProfileScreenState.Error -> Text("${screen.message} (${screen.code ?: "-"})")
        }
        Button(onClick = { viewModel.onAgeChanged(30) }) { Text("Set age") }
        Button(onClick = { viewModel.onAddressChanged("Oslo", 150) }) { Text("Set address") }
        Button(onClick = viewModel::onLoad) { Text("Load") }
        Button(onClick = viewModel::onBack) { Text("Back") }
        Button(onClick = { viewModel.onToast("Hello") }) { Text("Toast") }
        Text("Back taps: $backTaps, toast: $toast")
    }
}
