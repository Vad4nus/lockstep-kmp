import SampleShared
import SwiftUI

@available(iOS 17.0, *)
func check() {
    let vm = ProfileViewModel()
    let s: ProfileState = vm.state.value
    let kotlin: __ProfileState? = s.kotlin
    let age: Int32? = s.age
    let zip: Int32? = s.address?.zip
    let tags: [String] = s.tags
    switch s.screen {
    case .idle, .loading: break
    case .error(let message, let code): _ = (message, code)
    }
    let c: SampleShared.Category = s.category
    let fav: UiState<ListState> = FavoritesViewModel().state
    let set: UiState<SettingsState> = SettingsViewModel().state
    _ = s == s
    Task {
        for await a in vm.actions {
            switch a {
            case .navigateBack: break
            case .showToast(let text): _ = text
            }
        }
    }
    _ = vm.__state
    let address = Address(city: "Oslo", zip: 150)
    let label: String = address.label()
    let near: Bool = address.isNear(other: address)
    let zipOrNull: Int32? = address.zipOrNull()
    let next: Address? = address.next()
    let formatter: () -> String = address.formatter()
    let tag: String = address.tag(kotlin: "k")
    let copied: Address = address.doCopy(prefix: "p")
    address.log()
    let named: ProfileState = s.withName(name: "n")
    let screen: ProfileScreenState = s.screenOrIdle()
    _ = (label, near, zipOrNull, next, formatter, tag, copied, named, screen)
    _ = (kotlin, age, zip, tags, c, fav, set)
    let mapping = MappingFixtureViewModel().state.value
    let addresses: [Address] = mapping.addresses
    let maybe: [Address]? = mapping.maybeAddresses
    let title: String = mapping.title
    let maybeTitle: String? = mapping.maybeTitle
    let route: FixtureRoute = .item(payload: address)
    let kotlinRoute: __FixtureRoute = route.kotlinValue
    let back: FixtureRoute? = FixtureRoute(route: kotlinRoute)
    let hashed: Set<FixtureRoute> = [route]
    let rebuilt: __MappingFixtureState = MappingFixtureState(addresses: [address], maybeAddresses: nil, title: "t", maybeTitle: nil).kotlinValue
    let objectState: UiState<MappingFixtureState> = FixtureNavigation.shared.state
    _ = (addresses, maybe, title, maybeTitle, back, hashed, rebuilt, objectState)

    let flows = ValueFlows()
    let count: UiState<Int32> = flows.count
    let total: UiState<Int64?> = flows.total
    let enabled: UiState<Bool> = flows.enabled
    let flowTitle: UiState<String> = flows.title
    let flowMaybeTitle: UiState<String?> = flows.maybeTitle
    let flowLabel: UiState<String> = flows.label
    let mode: UiState<FixtureMode> = flows.mode
    let flowTags: UiState<[String]> = flows.tags
    let scores: UiState<[String: KotlinInt]> = flows.scores
    let flowAddresses: UiState<[Address]> = flows.addresses
    let flowMaybeAddresses: UiState<[Address]?> = flows.maybeAddresses
    let selected: UiState<Address?> = flows.selected
    let repository: UiState<OrdersRepository> = flows.repository
    let holder: UiState<FixtureStateHolder> = flows.holder
    let clicks: UiEvents<Int32> = flows.clicks
    let ticks: UiEvents<KotlinUnit> = flows.ticks
    _ = (count, total, enabled, flowTitle, flowMaybeTitle, flowLabel, mode, flowTags, scores, flowAddresses, flowMaybeAddresses)
    _ = (selected, repository, holder, clicks, ticks)

    let orders = OrdersViewModel(repo: OrdersRepository(), navigator: SampleGraph.shared.navigator)
    let ordersState: UiState<OrdersState> = orders.state
    let ordersChrome: UiState<ScreenChrome> = orders.chrome
    let ordersActions: UiEvents<OrdersAction> = orders.actions
    let ordersRoute: OrdersRoute = .filter(preset: "paid")
    let asRoute: any NavigationRoute = ordersRoute
    let kotlinOrdersRoute: Route = ordersRoute.kotlinRoute
    let owner = ViewModelOwner { OrderFilterViewModel(repo: OrdersRepository(), navigator: SampleGraph.shared.navigator, preset: nil) }
    let filterVm: OrderFilterViewModel = owner.viewModel
    let chrome: ScreenChrome = orders.chrome.value
    let loading: LoadingKind? = chrome.loading
    let background: Bool = chrome.backgroundLoading
    let error: UiError? = chrome.error
    let retryLabel: String? = error?.retryLabel
    SampleGraph.shared.navigator.onSystemPop(newPath: [kotlinOrdersRoute])
    let printed: any CustomStringConvertible = FormState(values: [:], errors: [:])
    _ = (ordersState, ordersChrome, ordersActions, asRoute, kotlinOrdersRoute, filterVm, loading, background, retryLabel, printed)
}

@available(iOS 17.0, *)
struct MvvmCheck: View {
    let viewModel: OrdersViewModel

    var body: some View {
        MvvmScreen(viewModel, screenName: "orders", onRefresh: { viewModel.refresh() }) { (state: OrdersState) in
            Text("\(state.orders.count)")
        }
        .environment(\.mvvmScreenTracker) { (name: String) in print(name) }
    }
}

@available(iOS 17.0, *)
struct NodeCheck: View {
    let viewModel: FixtureViewModel

    var body: some View {
        let state: FixtureState = viewModel.state.value
        NodeView(state.screen.root, form: state.form) { (event: NodeEvent) in viewModel.onEvent(event: event.kotlinValue) }
            .environment(\.nodeTheme, NodeTheme.standard)
    }
}

@available(iOS 17.0, *)
struct NavigationCheck: View {
    var body: some View {
        NavigatorStack(SampleGraph.shared.navigator) { (route: OrdersRoute) in Text(String(describing: route)) }
    }
}
