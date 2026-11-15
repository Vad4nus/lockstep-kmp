package io.github.vad4nus.lockstep.skie.render

import io.github.vad4nus.lockstep.skie.model.MvvmModel
import io.github.vad4nus.lockstep.skie.model.SwiftEnum

/** Swift types the MVVM runtime declares; no exported Kotlin class may take these names. */
internal val mvvmRuntimeSwiftTypes: List<String> =
    listOf("ViewModelOwner", "MvvmScreenStyle", "MvvmScreenModel", "MvvmScreen", "NavigationRoute", "NavigatorStack")

internal fun mvvmRuntimeSwift(model: MvvmModel, routes: List<SwiftEnum>): String {
    val blocks = listOf(screenRuntime(model), navigationRuntime(model)) +
        model.viewModels.map(::renderConformance) +
        routes.map(::renderRouteConformance)
    return "import Foundation\nimport SwiftUI\nimport UIKit\n\n" + blocks.joinToString("\n\n") + "\n"
}

private fun screenRuntime(model: MvvmModel): String = """
    @available(iOS 17.0, *)
    public final class ViewModelOwner<VM: ${model.androidViewModel}> {
        private let store = ${model.viewModelStore}()
        private let factory: () -> VM
        private var created: VM?

        public init(_ factory: @escaping () -> VM) {
            self.factory = factory
        }

        public var viewModel: VM {
            if let created { return created }
            let viewModel = store.getOrPut(key: "lockstep.viewModel", factory: factory) as! VM
            created = viewModel
            return viewModel
        }

        deinit {
            store.clear()
        }
    }

    /// The pieces `MvvmScreen` draws around the content. `error` gets a retry action when the error has a retry label.
    @available(iOS 17.0, *)
    public struct MvvmScreenStyle {
        public var loading: () -> AnyView
        public var backgroundLoading: () -> AnyView
        public var error: (${model.uiError}, (() -> Void)?) -> AnyView
        public var toast: (${model.uiMessage}, (() -> Void)?) -> AnyView

        public init(
            loading: @escaping () -> AnyView,
            backgroundLoading: @escaping () -> AnyView,
            error: @escaping (${model.uiError}, (() -> Void)?) -> AnyView,
            toast: @escaping (${model.uiMessage}, (() -> Void)?) -> AnyView
        ) {
            self.loading = loading
            self.backgroundLoading = backgroundLoading
            self.error = error
            self.toast = toast
        }

        public static let standard = MvvmScreenStyle(
            loading: {
                AnyView(
                    ProgressView()
                        .controlSize(.large)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.black.opacity(0.08))
                )
            },
            backgroundLoading: {
                AnyView(ProgressView().progressViewStyle(.linear))
            },
            error: { error, retry in
                AnyView(
                    ScrollView {
                        VStack(spacing: 16) {
                            Text(error.message).multilineTextAlignment(.center)
                            if let retry, let label = error.retryLabel {
                                Button(label, action: retry).buttonStyle(.borderedProminent)
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 240)
                        .padding()
                    }
                )
            },
            toast: { message, action in
                let failed = message.kind == .error
                return AnyView(
                    HStack(spacing: 12) {
                        Text(message.text)
                        if let label = message.actionLabel, let action {
                            Button(label, action: action)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .foregroundStyle(failed ? Color.white : Color.primary)
                    .tint(failed ? Color.white : Color.accentColor)
                    .background(
                        failed ? AnyShapeStyle(Color.red) : AnyShapeStyle(.regularMaterial),
                        in: RoundedRectangle(cornerRadius: 12)
                    )
                    .padding()
                )
            }
        )
    }

    @available(iOS 17.0, *)
    private struct MvvmScreenStyleKey: EnvironmentKey {
        static let defaultValue = MvvmScreenStyle.standard
    }

    @available(iOS 17.0, *)
    private struct MvvmScreenTrackerKey: EnvironmentKey {
        static let defaultValue: ((String) -> Void)? = nil
    }

    @available(iOS 17.0, *)
    extension EnvironmentValues {
        public var mvvmScreenStyle: MvvmScreenStyle {
            get { self[MvvmScreenStyleKey.self] }
            set { self[MvvmScreenStyleKey.self] = newValue }
        }

        /// Receives the name of every `MvvmScreen` that becomes visible and has a `screenName`.
        public var mvvmScreenTracker: ((String) -> Void)? {
            get { self[MvvmScreenTrackerKey.self] }
            set { self[MvvmScreenTrackerKey.self] = newValue }
        }
    }

    @available(iOS 17.0, *)
    public protocol MvvmScreenModel: AnyObject {
        associatedtype S
        associatedtype A
        var state: UiState<S> { get }
        var chrome: UiState<${model.chrome}> { get }
        var actions: UiEvents<A> { get }
        func screenAppeared()
        func screenDisappeared()
        func retry()
        func messageShown(id: Swift.Int64)
        func messageActionClicked(id: Swift.Int64)
        func dialogResult(id: Swift.Int64, result: DialogResult)
    }

    @available(iOS 17.0, *)
    private struct MvvmRefresh: ViewModifier {
        let action: (() async -> Void)?

        func body(content: Content) -> some View {
            if let action {
                content.refreshable(action: action)
            } else {
                content
            }
        }
    }

    /// A screen bound to a ViewModel: content or the error view, the blocking loader over either of them,
    /// the background loading indicator, toasts, the dialog, the lifecycle hooks and one-shot actions.
    ///
    /// The screen counts as visible while it is on screen and the app is not in the background, as on Android;
    /// that also holds for a screen inside a `UIHostingController`.
    /// `onRefresh` adds pull to refresh to the scroll views inside; the indicator stays until the refresh
    /// request ends. A toast stays for its duration, is announced to VoiceOver, and leaves the queue when
    /// the time is up, when its action is tapped, or when the screen it is on goes away.
    /// The dialog reports the button the user taps; an alert has no other way to close.
    @available(iOS 17.0, *)
    public struct MvvmScreen<VM: MvvmScreenModel, Content: View>: View {
        private let viewModel: VM
        private let screenName: String?
        private let onRefresh: (() -> Void)?
        private let onAction: (VM.A) -> Void
        private let content: (VM.S) -> Content
        @Environment(\.mvvmScreenStyle) private var style
        @Environment(\.mvvmScreenTracker) private var tracker
        @State private var onScreen = false
        @State private var visible = false

        public init(
            _ viewModel: VM,
            screenName: String? = nil,
            onRefresh: (() -> Void)? = nil,
            onAction: @escaping (VM.A) -> Void = { _ in },
            @ViewBuilder content: @escaping (VM.S) -> Content
        ) {
            self.viewModel = viewModel
            self.screenName = screenName
            self.onRefresh = onRefresh
            self.onAction = onAction
            self.content = content
        }

        private func show(_ shown: Bool) {
            guard shown != visible else { return }
            visible = shown
            if shown {
                viewModel.screenAppeared()
                if let screenName { tracker?(screenName) }
            } else {
                viewModel.screenDisappeared()
            }
        }

        private func refresh() async {
            onRefresh?()
            repeat {
                try? await Task.sleep(nanoseconds: 100_000_000)
            } while !Task.isCancelled && viewModel.chrome.value.loading == .refresh
        }

        public var body: some View {
            let chrome = viewModel.chrome
            let current = chrome.value
            let head = current.messages.first
            let dialog = current.dialog
            ZStack {
                if let error = current.error {
                    style.error(error, error.retryLabel == nil ? nil : { viewModel.retry() })
                } else {
                    content(viewModel.state.value)
                }
                if current.loading == .blocking {
                    style.loading()
                }
            }
            .modifier(MvvmRefresh(action: onRefresh == nil ? nil : refresh))
            .overlay(alignment: .top) {
                if current.backgroundLoading {
                    style.backgroundLoading()
                }
            }
            .overlay(alignment: .bottom) {
                if let head {
                    style.toast(head, head.actionLabel == nil ? nil : { viewModel.messageActionClicked(id: head.id) })
                }
            }
            .alert(
                dialog?.title ?? "",
                isPresented: Binding(get: { chrome.value.dialog != nil }, set: { _ in }),
                presenting: dialog
            ) { dialog in
                Button(dialog.confirm, role: dialog.destructive ? .destructive : nil) { viewModel.dialogResult(id: dialog.id, result: .confirm) }
                if let dismiss = dialog.dismiss {
                    Button(dismiss, role: .cancel) { viewModel.dialogResult(id: dialog.id, result: .dismiss) }
                }
            } message: { dialog in
                Text(dialog.text)
            }
            .onAppear {
                onScreen = true
                show(UIApplication.shared.applicationState != .background)
            }
            .onDisappear {
                onScreen = false
                show(false)
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)) { _ in
                show(false)
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
                show(onScreen)
            }
            .task {
                for await action in viewModel.actions { onAction(action) }
            }
            .task(id: head?.id) {
                guard let head else { return }
                UIAccessibility.post(notification: .announcement, argument: head.text)
                try? await Task.sleep(nanoseconds: UInt64(max(0, head.durationMillis)) * 1_000_000)
                viewModel.messageShown(id: head.id)
            }
        }
    }
""".trimIndent()

private fun navigationRuntime(model: MvvmModel): String = """
    @available(iOS 17.0, *)
    public protocol NavigationRoute: Hashable, Identifiable {
        init?(route: ${model.route})
        var kotlinRoute: ${model.route} { get }
    }

    @available(iOS 17.0, *)
    extension NavigationRoute {
        public var id: Self { self }
    }

    /// Draws a navigator with `NavigationStack`. A route the route type cannot express stops the path in front
    /// of it; debug builds also stop on an assertion.
    @available(iOS 17.0, *)
    public struct NavigatorStack<R: NavigationRoute, Destination: View>: View {
        private let navigator: ${model.navigator}
        private let stack: UiState<${model.navStack}>
        private let destination: (R) -> Destination

        public init(_ navigator: ${model.navigator}, @ViewBuilder destination: @escaping (R) -> Destination) {
            self.navigator = navigator
            self.stack = navigator.stack
            self.destination = destination
        }

        private func convert(_ route: ${model.route}) -> R? {
            if let converted = R(route: route) { return converted }
            assertionFailure("lockstep: \(type(of: route)) is not a route of \(R.self)")
            return nil
        }

        public var body: some View {
            let current = stack.value
            let root = convert(current.root)
            let path = current.path.map { convert(${'$'}0) }.prefix { ${'$'}0 != nil }.compactMap { ${'$'}0 }
            let sheet = current.sheet.flatMap { convert(${'$'}0) }
            NavigationStack(
                path: Binding(
                    get: { path },
                    set: { newPath in navigator.onSystemPop(newPath: newPath.map { ${'$'}0.kotlinRoute }) }
                )
            ) {
                Group {
                    if let root { destination(root) }
                }
                .navigationDestination(for: R.self) { route in destination(route) }
            }
            .sheet(
                item: Binding<R?>(
                    get: { sheet },
                    set: { newValue in
                        if newValue == nil, let shown = sheet { navigator.onSystemDismissSheet(expected: shown.kotlinRoute) }
                    }
                )
            ) { route in
                destination(route)
            }
        }
    }
""".trimIndent()

private fun renderConformance(viewModel: String): String = """
    @available(iOS 17.0, *)
    extension $viewModel: MvvmScreenModel {}
""".trimIndent()

private fun renderRouteConformance(route: SwiftEnum): String = """
    @available(iOS 17.0, *)
    extension ${route.name}: NavigationRoute {
        public var kotlinRoute: ${route.routeSwiftName} { kotlinValue }
    }
""".trimIndent()
