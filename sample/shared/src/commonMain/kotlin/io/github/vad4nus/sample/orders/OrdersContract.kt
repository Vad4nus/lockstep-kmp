package io.github.vad4nus.sample.orders

import io.github.vad4nus.lockstep.mvvm.navigation.ResultKey
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.lockstep.mvvm.navigation.SheetRoute

data class Order(val id: Int, val title: String, val status: String)

sealed interface OrdersView {
    data object All : OrdersView
    data class Filtered(val status: String) : OrdersView
}

data class OrdersState(val orders: List<Order> = emptyList(), val view: OrdersView = OrdersView.All)

sealed interface OrdersAction {
    data object ScrollToTop : OrdersAction
}

data class OrdersDetailsState(val order: Order? = null, val note: String = "")

data class OrderFilterState(val statuses: List<String> = emptyList(), val selected: String? = null)

sealed interface OrdersRoute : Route {
    data object List : OrdersRoute
    data class Details(val id: Int) : OrdersRoute
    data object Profile : OrdersRoute
    data class Filter(val preset: String? = null) : OrdersRoute, SheetRoute
    data class Fixture(val name: String) : OrdersRoute
}

val FilterResult = ResultKey<String>("orders.filter")
