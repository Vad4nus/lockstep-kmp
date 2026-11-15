import SampleShared
import SwiftUI

struct OrdersRoot: View {
    var body: some View {
        NavigatorStack(SampleGraph.shared.navigator) { (route: OrdersRoute) in
            switch route {
            case .list: OrdersListScreen()
            case .details(let id): OrderDetailsScreen(id: id)
            case .profile: ProfileScreen()
            case .filter(let preset): OrderFilterSheet(preset: preset)
            case .fixture(let name): FixtureScreen(name: name)
            }
        }
    }
}

struct OrdersListScreen: View {
    @State private var owner = ViewModelOwner {
        OrdersViewModel(repo: SampleGraph.shared.repo, navigator: SampleGraph.shared.navigator)
    }

    var body: some View {
        let viewModel = owner.viewModel
        MvvmScreen(viewModel, screenName: "orders", onRefresh: { viewModel.refresh() }) { state in
            List {
                if case .filtered(let status) = state.view {
                    HStack {
                        Text("Filtered: \(status)")
                        Spacer()
                        Button("Clear") { viewModel.onClearFilter() }
                    }
                }
                ForEach(state.orders, id: \.id) { order in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(order.title)
                            Text(order.status).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button("Delete") { viewModel.onDelete(id: order.id) }
                            .buttonStyle(.borderless)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { viewModel.onOrderClicked(id: order.id) }
                }
            }
        }
        .navigationTitle("Orders")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button("Filter") { viewModel.onFilterClicked() } }
            ToolbarItem(placement: .topBarTrailing) { Button("Profile") { viewModel.onProfileClicked() } }
            ToolbarItem(placement: .topBarTrailing) { Button("Screens") { viewModel.onScreensClicked() } }
        }
    }
}

struct OrderDetailsScreen: View {
    private let id: Int32
    @State private var owner: ViewModelOwner<OrderDetailsViewModel>

    init(id: Int32) {
        self.id = id
        _owner = State(initialValue: ViewModelOwner {
            OrderDetailsViewModel(id: id, repo: SampleGraph.shared.repo, navigator: SampleGraph.shared.navigator)
        })
    }

    var body: some View {
        let viewModel = owner.viewModel
        MvvmScreen(viewModel) { state in
            Form {
                Text(state.order?.title ?? "")
                Text(state.order?.status ?? "")
                TextField("Note", text: Binding(
                    get: { state.note },
                    set: { viewModel.onNoteChanged(note: $0) }
                ))
            }
        }
        .navigationTitle("Order \(id)")
    }
}

struct OrderFilterSheet: View {
    private let preset: String?
    @State private var owner: ViewModelOwner<OrderFilterViewModel>

    init(preset: String?) {
        self.preset = preset
        _owner = State(initialValue: ViewModelOwner {
            OrderFilterViewModel(repo: SampleGraph.shared.repo, navigator: SampleGraph.shared.navigator, preset: preset)
        })
    }

    var body: some View {
        let viewModel = owner.viewModel
        MvvmScreen(viewModel) { state in
            VStack(alignment: .leading, spacing: 12) {
                Text("Filter orders" + (preset.map { " (\($0))" } ?? "")).font(.headline)
                ForEach(state.statuses, id: \.self) { status in
                    Button(status) { viewModel.onSelect(status: status) }
                }
                Button("Only paid") { viewModel.onOnlyPaid() }.buttonStyle(.borderedProminent)
                Button("Open first order") { viewModel.onOpenFirstOrder() }.buttonStyle(.borderedProminent)
                Button("Open first order in 300 ms") { viewModel.onOpenFirstOrderLater() }.buttonStyle(.borderedProminent)
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .presentationDetents([.medium, .large])
    }
}
