package io.github.vad4nus.sample.orders

import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.mvvm.DialogResult
import io.github.vad4nus.lockstep.mvvm.ErrorDisplay
import io.github.vad4nus.lockstep.mvvm.LoadingKind
import io.github.vad4nus.lockstep.mvvm.MessageAction
import io.github.vad4nus.lockstep.mvvm.MessageKind
import io.github.vad4nus.lockstep.mvvm.MvvmViewModel
import io.github.vad4nus.lockstep.mvvm.navigation.StackNavigator
import io.github.vad4nus.lockstep.result.AppError
import io.github.vad4nus.lockstep.result.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OrdersRepository {
    private val orders = listOf(
        Order(1, "Keyboard", "paid"),
        Order(2, "Monitor", "new"),
        Order(3, "Desk", "shipped"),
    )
    private var ordersCalls = 0
    private var statusCalls = 0

    suspend fun orders(): AppResult<List<Order>> {
        delay(600)
        return if (ordersCalls++ == 0) AppResult.Failure(AppError.NoConnection) else AppResult.Success(orders)
    }

    suspend fun order(id: Int): AppResult<Order> {
        delay(300)
        return orders.firstOrNull { it.id == id }?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.Http(404, "Order not found"))
    }

    suspend fun statuses(): AppResult<List<String>> {
        delay(400)
        return if (statusCalls++ == 0) {
            AppResult.Failure(AppError.Timeout)
        } else {
            AppResult.Success(listOf("new", "paid", "shipped", "cancelled"))
        }
    }

    fun count(status: String): Int = orders.count { it.status == status }
}

object SampleGraph {
    val navigator = StackNavigator(OrdersRoute.List)
    val repo = OrdersRepository()

    private val log = mutableListOf<String>()
    private val instances = mutableMapOf<String, Int>()

    val lifecycleLog: List<String> get() = log.toList()

    internal fun created(name: String): Int {
        val serial = (instances[name] ?: 0) + 1
        instances[name] = serial
        record("$name#$serial created")
        return serial
    }

    internal fun cleared(name: String, serial: Int) = record("$name#$serial cleared")

    private fun record(line: String) {
        log += line
        println("[lifecycle] $line")
    }
}

abstract class SampleViewModel<S : Any, A : Any>(initialState: S) : MvvmViewModel<S, A>(initialState) {
    private val name = this::class.simpleName.orEmpty()
    private val serial = SampleGraph.created(name)

    /** How many times the screen became visible; the iOS tests read it. */
    var appearances = 0
        private set

    /** How many times the screen stopped being visible; the iOS tests read it. */
    var disappearances = 0
        private set

    override fun onAppear() {
        appearances++
    }

    override fun onDisappear() {
        disappearances++
    }

    override fun onCleared() {
        SampleGraph.cleared(name, serial)
        super.onCleared()
    }
}

class OrdersViewModel(
    private val repo: OrdersRepository,
    private val navigator: StackNavigator,
) : SampleViewModel<OrdersState, OrdersAction>(OrdersState()) {

    private var all = emptyList<Order>()

    init {
        navigator.results(FilterResult)
            .onEach { status ->
                updateState { copy(view = OrdersView.Filtered(status)) }
                publish()
            }
            .launchIn(viewModelScope)
    }

    override fun onFirstAppear() = load()

    fun load() {
        request(call = { repo.orders() }, key = "load", errors = ErrorDisplay.Screen) {
            all = it
            publish()
        }
    }

    fun refresh() {
        request(call = { repo.orders() }, loading = LoadingKind.Refresh, key = "load") {
            all = it
            publish()
        }
    }

    fun onOrderClicked(id: Int) = navigator.push(OrdersRoute.Details(id))

    fun onFilterClicked() = navigator.push(OrdersRoute.Filter())

    fun onProfileClicked() = navigator.push(OrdersRoute.Profile)

    fun onScreensClicked() = navigator.push(OrdersRoute.Fixture("login"))

    fun onClearFilter() {
        updateState { copy(view = OrdersView.All) }
        publish()
    }

    /** Asks before deleting, and asks once more about a paid order: the second dialog opens from the first one's callback. */
    fun onDelete(id: Int) {
        val order = all.firstOrNull { it.id == id } ?: return
        confirm("Delete ${order.title}?") {
            if (order.status == "paid") confirm("${order.title} is paid. Delete it anyway?") { delete(order) } else delete(order)
        }
    }

    private fun confirm(text: String, onConfirm: () -> Unit) {
        showDialog(
            text = StringDesc.Raw(text),
            confirm = StringDesc.Raw("Delete"),
            dismiss = StringDesc.Raw("Cancel"),
            destructive = true,
        ) { if (it == DialogResult.Confirm) onConfirm() }
    }

    private fun delete(order: Order) {
        all = all - order
        publish()
        showMessage(
            StringDesc.Raw("Order deleted"),
            MessageKind.Success,
            MessageAction(StringDesc.Raw("Undo")) {
                all = (all + order).sortedBy { it.id }
                publish()
            },
        )
    }

    private fun publish() {
        updateState {
            copy(orders = (view as? OrdersView.Filtered)?.let { filter -> all.filter { it.status == filter.status } } ?: all)
        }
    }
}

class OrderDetailsViewModel(
    private val id: Int,
    private val repo: OrdersRepository,
    private val navigator: StackNavigator,
) : SampleViewModel<OrdersDetailsState, OrdersAction>(OrdersDetailsState()) {

    override fun onFirstAppear() = load()

    fun load() {
        request(call = { repo.order(id) }, errors = ErrorDisplay.Screen) { updateState { copy(order = it) } }
    }

    fun onNoteChanged(note: String) = updateState { copy(note = note) }

    fun onBack() {
        navigator.pop()
    }
}

class OrderFilterViewModel(
    private val repo: OrdersRepository,
    private val navigator: StackNavigator,
    preset: String? = null,
) : SampleViewModel<OrderFilterState, OrdersAction>(OrderFilterState(selected = preset)) {

    override fun onFirstAppear() = load()

    fun load() {
        request(call = { repo.statuses() }, errors = ErrorDisplay.Screen) { updateState { copy(statuses = it) } }
    }

    fun onSelect(status: String) {
        if (repo.count(status) == 0) {
            showMessage(StringDesc.Raw("No orders with status $status"))
        } else {
            navigator.popWithResult(FilterResult, status)
        }
    }

    fun onOnlyPaid() = navigator.push(OrdersRoute.Filter("paid"))

    fun onOpenFirstOrder() = navigator.push(OrdersRoute.Details(1))

    fun onOpenFirstOrderLater() {
        viewModelScope.launch {
            delay(300)
            onOpenFirstOrder()
        }
    }
}
