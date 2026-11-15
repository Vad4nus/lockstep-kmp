import SampleShared
import SwiftUI

/// Keeps a ViewModel that is not an AndroidX one for as long as the screen lives, and clears it afterwards.
private final class ProfileOwner {
    let viewModel = ProfileViewModel()

    deinit {
        viewModel.clear()
    }
}

struct ProfileScreen: View {
    @State private var owner = ProfileOwner()
    @State private var backTaps = 0
    @State private var toast = ""

    var body: some View {
        let viewModel = owner.viewModel
        let state = viewModel.state
        Form {
            TextField("Name", text: Binding(
                get: { state.value.name },
                set: { viewModel.onNameChanged(name: $0) }
            ))
            Text("Age: \(state.value.age.map { String($0) } ?? "-")")
            Text("City: \(state.value.address?.city ?? "-")")
            screenStatus(state.value.screen)
            Button("Set age") { viewModel.onAgeChanged(age: 30) }
            Button("Set address") { viewModel.onAddressChanged(city: "Oslo", zip: 150) }
            Button("Load") { viewModel.onLoad() }
            Button("Back") { viewModel.onBack() }
            Button("Toast") { viewModel.onToast(text: "Hello") }
            Text("Back taps: \(backTaps), toast: \(toast)")
        }
        .task {
            for await action in viewModel.actions {
                switch action {
                case .navigateBack: backTaps += 1
                case .showToast(let text): toast = text
                }
            }
        }
    }

    @ViewBuilder
    private func screenStatus(_ screen: ProfileScreenState) -> some View {
        switch screen {
        case .idle: Text("Idle")
        case .loading: ProgressView()
        case .error(let message, let code): Text("\(message) (\(code.map { String($0) } ?? "-"))")
        }
    }
}
