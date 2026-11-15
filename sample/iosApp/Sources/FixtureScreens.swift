import SampleShared
import SwiftUI

struct FixtureScreen: View {
    private let name: String
    @State private var owner: ViewModelOwner<FixtureViewModel>

    init(name: String) {
        self.name = name
        _owner = State(initialValue: ViewModelOwner {
            FixtureViewModel(name: name, navigator: SampleGraph.shared.navigator)
        })
    }

    var body: some View {
        let viewModel = owner.viewModel
        MvvmScreen(viewModel) { state in
            NodeView(state.screen.root, form: state.form) { viewModel.onEvent(event: $0.kotlinValue) }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .environment(\.nodeTheme, fixtureTheme)
        .navigationTitle(name)
    }
}

private let fixtureTheme: NodeTheme = {
    var theme = NodeTheme.standard
    theme.images = ["star": Image(uiImage: UIImage(systemName: "star.fill")!.withTintColor(.systemYellow, renderingMode: .alwaysOriginal))]
    theme.custom = ["badge": { props in AnyView(Badge(label: props["label"] ?? "")) }]
    return theme
}()

private struct Badge: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.callout.weight(.medium))
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .background(Color.orange.opacity(0.2), in: Capsule())
    }
}
