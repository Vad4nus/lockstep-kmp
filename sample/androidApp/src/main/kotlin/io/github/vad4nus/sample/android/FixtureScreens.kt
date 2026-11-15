package io.github.vad4nus.sample.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vad4nus.lockstep.compose.MvvmScreen
import io.github.vad4nus.lockstep.compose.screens.LocalNodeTheme
import io.github.vad4nus.lockstep.compose.screens.NodeTheme
import io.github.vad4nus.lockstep.compose.screens.RenderNode
import io.github.vad4nus.sample.orders.FixtureViewModel
import io.github.vad4nus.sample.orders.SampleGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixtureScreen(name: String) {
    val viewModel = viewModel { FixtureViewModel(name, SampleGraph.navigator) }
    val star = painterResource(R.drawable.star)
    val theme = remember(star) {
        NodeTheme(images = mapOf("star" to star), custom = mapOf("badge" to { props -> Badge(props["label"].orEmpty()) }))
    }
    MvvmScreen(
        viewModel = viewModel,
        screenName = "fixture-$name",
        imeAware = true,
        topBar = { TopAppBar(title = { Text(name) }) },
    ) { state, padding ->
        CompositionLocalProvider(LocalNodeTheme provides theme) {
            RenderNode(state.screen.root, state.form, viewModel::onEvent, Modifier.padding(padding))
        }
    }
}

@Composable
private fun Badge(label: String) {
    Text(
        label,
        Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.labelLarge,
    )
}
