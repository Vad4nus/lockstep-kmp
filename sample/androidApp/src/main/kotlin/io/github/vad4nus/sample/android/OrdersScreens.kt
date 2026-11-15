package io.github.vad4nus.sample.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import io.github.vad4nus.lockstep.compose.MvvmScreen
import io.github.vad4nus.lockstep.compose.MvvmSheet
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.sample.orders.OrderDetailsViewModel
import io.github.vad4nus.sample.orders.OrderFilterViewModel
import io.github.vad4nus.sample.orders.OrdersRoute
import io.github.vad4nus.sample.orders.OrdersView
import io.github.vad4nus.sample.orders.OrdersViewModel
import io.github.vad4nus.sample.orders.SampleGraph

val ordersEntryProvider: (Route) -> NavEntry<Route> = entryProvider {
    entry<OrdersRoute.List> { OrdersListScreen() }
    entry<OrdersRoute.Details> { OrderDetailsScreen(it.id) }
    entry<OrdersRoute.Profile> { ProfileScreen() }
    entry<OrdersRoute.Filter> { OrderFilterSheet(it) }
    entry<OrdersRoute.Fixture> { FixtureScreen(it.name) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScreen() {
    val viewModel = viewModel { OrdersViewModel(SampleGraph.repo, SampleGraph.navigator) }
    MvvmScreen(
        viewModel = viewModel,
        screenName = "orders",
        onRefresh = viewModel::refresh,
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                actions = {
                    TextButton(onClick = viewModel::onFilterClicked) { Text("Filter") }
                    TextButton(onClick = viewModel::onProfileClicked) { Text("Profile") }
                    TextButton(onClick = viewModel::onScreensClicked) { Text("Screens") }
                },
            )
        },
    ) { state, padding ->
        LazyColumn(contentPadding = padding) {
            (state.view as? OrdersView.Filtered)?.let { filtered ->
                item {
                    ListItem(
                        headlineContent = { Text("Filtered: ${filtered.status}") },
                        trailingContent = { TextButton(onClick = viewModel::onClearFilter) { Text("Clear") } },
                    )
                }
            }
            items(state.orders, key = { it.id }) { order ->
                ListItem(
                    headlineContent = { Text(order.title) },
                    supportingContent = { Text(order.status) },
                    trailingContent = { TextButton(onClick = { viewModel.onDelete(order.id) }) { Text("Delete") } },
                    modifier = Modifier.clickable { viewModel.onOrderClicked(order.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(id: Int) {
    val viewModel = viewModel { OrderDetailsViewModel(id, SampleGraph.repo, SampleGraph.navigator) }
    MvvmScreen(
        viewModel = viewModel,
        screenName = "details",
        imeAware = true,
        topBar = {
            TopAppBar(
                title = { Text("Order $id") },
                navigationIcon = { TextButton(onClick = viewModel::onBack) { Text("Back") } },
            )
        },
    ) { state, padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(state.order?.title.orEmpty())
            Text(state.order?.status.orEmpty())
            OutlinedTextField(value = state.note, onValueChange = viewModel::onNoteChanged, label = { Text("Note") })
        }
    }
}

@Composable
fun OrderFilterSheet(route: OrdersRoute.Filter) {
    val viewModel = viewModel { OrderFilterViewModel(SampleGraph.repo, SampleGraph.navigator, route.preset) }
    MvvmSheet(viewModel = viewModel, screenName = "filter") { state ->
        Column(Modifier.padding(16.dp)) {
            Text("Filter orders${route.preset?.let { " ($it)" }.orEmpty()}")
            state.statuses.forEach { status ->
                TextButton(onClick = { viewModel.onSelect(status) }) { Text(status) }
            }
            Button(onClick = viewModel::onOnlyPaid) { Text("Only paid") }
            Button(onClick = viewModel::onOpenFirstOrder) { Text("Open first order") }
            Button(onClick = viewModel::onOpenFirstOrderLater) { Text("Open first order in 300 ms") }
        }
    }
}
