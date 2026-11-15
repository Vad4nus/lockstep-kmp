import Observation
import SampleShared
import SwiftUI
import XCTest

@testable import Sample

@Observable
final class Redraws {
    var count = 0
}

private struct RedrawChild: View {
    nonisolated(unsafe) static var inits = 0
    @State private var owner = ViewModelOwner {
        OrderFilterViewModel(repo: OrdersRepository(), navigator: StackNavigator(root: OrdersRoute.list.kotlinValue), preset: nil)
    }

    init() {
        Self.inits += 1
    }

    var body: some View {
        Text("\(owner.viewModel.state.value.statuses.count)")
    }
}

private struct RedrawParent: View {
    let redraws: Redraws

    var body: some View {
        VStack {
            Text("\(redraws.count)")
            RedrawChild()
        }
    }
}

@MainActor
final class MvvmScreenTests: XCTestCase {

    /// Lets the run loop turn for a fixed time: only for the first render and for checks that nothing happens.
    private func settle(_ seconds: TimeInterval = 1.0) {
        RunLoop.main.run(until: Date().addingTimeInterval(seconds))
    }

    private func waitUntil(_ description: String, timeout: TimeInterval = 5, _ condition: () -> Bool) {
        let deadline = Date().addingTimeInterval(timeout)
        while !condition() && Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.05))
        }
        XCTAssertTrue(condition(), description)
    }

    private func log(since index: Int) -> [String] {
        SampleGraph.shared.lifecycleLog.dropFirst(index).map { $0.replacingOccurrences(of: #"#\d+"#, with: "", options: .regularExpression) }
    }

    private func find<T: UIViewController>(_ type: T.Type, in controller: UIViewController?) -> T? {
        guard let controller else { return nil }
        if let match = controller as? T { return match }
        for child in controller.children + [controller.presentedViewController].compactMap({ $0 }) {
            if let match = find(type, in: child) { return match }
        }
        return nil
    }

    private func host<V: View>(_ view: V) -> UIWindow {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first!
        let window = UIWindow(windowScene: scene)
        window.rootViewController = UIHostingController(rootView: view)
        window.makeKeyAndVisible()
        settle(0.3)
        return window
    }

    private func newNavigator() -> StackNavigator {
        StackNavigator(root: OrdersRoute.list.kotlinValue)
    }

    func testOwnerDeinitClearsTheStore() {
        let start = SampleGraph.shared.lifecycleLog.count
        var owner: ViewModelOwner<OrderFilterViewModel>? = ViewModelOwner {
            OrderFilterViewModel(repo: OrdersRepository(), navigator: self.newNavigator(), preset: nil)
        }
        XCTAssertEqual(log(since: start), [])

        _ = owner?.viewModel
        XCTAssertEqual(log(since: start), ["OrderFilterViewModel created"])

        owner = nil
        XCTAssertEqual(log(since: start), ["OrderFilterViewModel created", "OrderFilterViewModel cleared"])
    }

    func testRedrawingTheParentCreatesOneViewModel() {
        let start = SampleGraph.shared.lifecycleLog.count
        RedrawChild.inits = 0
        let redraws = Redraws()
        let window = host(RedrawParent(redraws: redraws))

        for redraw in 1...3 {
            let before = RedrawChild.inits
            redraws.count += 1
            waitUntil("redraw \(redraw)") { RedrawChild.inits > before }
        }

        XCTAssertEqual(log(since: start), ["OrderFilterViewModel created"])
        window.isHidden = true
    }

    func testMessageQueueShrinksWhenShown() {
        let viewModel = OrderFilterViewModel(repo: OrdersRepository(), navigator: newNavigator(), preset: nil)
        viewModel.onSelect(status: "cancelled")

        let messages = viewModel.chrome.value.messages
        XCTAssertEqual(messages.map(\.text), ["No orders with status cancelled"])

        viewModel.messageShown(id: messages[0].id)
        XCTAssertEqual(viewModel.chrome.value.messages.count, 0)
    }

    func testDialogConfirmRunsTheViewModelCallback() {
        let viewModel = OrdersViewModel(repo: OrdersRepository(), navigator: newNavigator())
        viewModel.load()
        waitUntil("first load fails") { viewModel.chrome.value.error != nil }
        XCTAssertNil(viewModel.chrome.value.loading)
        viewModel.retry()
        waitUntil("orders loaded") { viewModel.state.value.orders.count == 3 }

        viewModel.onDelete(id: 2)
        let dialog = viewModel.chrome.value.dialog
        XCTAssertEqual(dialog?.text, "Delete Monitor?")

        viewModel.dialogResult(id: dialog!.id, result: .confirm)
        XCTAssertEqual(viewModel.state.value.orders.map(\.id), [1, 3])
        XCTAssertNil(viewModel.chrome.value.dialog)

        let message = viewModel.chrome.value.messages.first
        XCTAssertEqual(message?.actionLabel, "Undo")
        viewModel.messageActionClicked(id: message!.id)
        XCTAssertEqual(viewModel.state.value.orders.map(\.id), [1, 2, 3])
    }

    func testAConfirmCallbackCanOpenASecondDialog() {
        let viewModel = loadedOrders()

        viewModel.onDelete(id: 1)
        let first = viewModel.chrome.value.dialog
        XCTAssertEqual(first?.text, "Delete Keyboard?")

        viewModel.dialogResult(id: first!.id, result: .confirm)
        let second = viewModel.chrome.value.dialog
        XCTAssertEqual(second?.text, "Keyboard is paid. Delete it anyway?")

        viewModel.dialogResult(id: first!.id, result: .dismiss)
        XCTAssertEqual(viewModel.chrome.value.dialog?.id, second?.id)

        viewModel.dialogResult(id: second!.id, result: .confirm)
        XCTAssertEqual(viewModel.state.value.orders.map(\.id), [2, 3])
    }

    func testTheDialogIsPresentedAsAnAlertAndLeavesWithIt() {
        let viewModel = loadedOrders()
        let window = host(MvvmScreen(viewModel) { state in Text("\(state.orders.count)") })

        viewModel.onDelete(id: 2)
        waitUntil("alert presented") { self.find(UIAlertController.self, in: window.rootViewController) != nil }
        let alert = find(UIAlertController.self, in: window.rootViewController)
        XCTAssertEqual(alert?.message, "Delete Monitor?")
        XCTAssertEqual(alert?.actions.map(\.title), ["Delete", "Cancel"])
        XCTAssertEqual(alert?.actions.map(\.style), [.destructive, .cancel])

        viewModel.dialogResult(id: viewModel.chrome.value.dialog!.id, result: .dismiss)
        waitUntil("alert dismissed") { self.find(UIAlertController.self, in: window.rootViewController) == nil }
        XCTAssertEqual(viewModel.state.value.orders.count, 3)
        window.isHidden = true
    }

    func testTheScreenFollowsTheAppBetweenForegroundAndBackground() {
        let viewModel = OrderFilterViewModel(repo: OrdersRepository(), navigator: newNavigator(), preset: nil)
        var tracked: [String] = []
        let window = host(
            MvvmScreen(viewModel, screenName: "filter") { _ in Text("filter") }
                .environment(\.mvvmScreenTracker) { tracked.append($0) }
        )
        waitUntil("appeared") { viewModel.appearances == 1 }

        NotificationCenter.default.post(name: UIApplication.didEnterBackgroundNotification, object: nil)
        waitUntil("hidden in the background") { viewModel.disappearances == 1 }

        NotificationCenter.default.post(name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.post(name: UIApplication.willEnterForegroundNotification, object: nil)
        waitUntil("shown again") { viewModel.appearances == 2 }
        XCTAssertEqual(viewModel.disappearances, 1)
        XCTAssertEqual(tracked, ["filter", "filter"])

        window.rootViewController = UIViewController()
        waitUntil("removed") { viewModel.disappearances == 2 }
        XCTAssertEqual(viewModel.appearances, 2)
        window.isHidden = true
    }

    func testAMessageLeavesTheQueueWhenItsTimeIsUp() {
        let viewModel = OrderFilterViewModel(repo: OrdersRepository(), navigator: newNavigator(), preset: nil)
        let window = host(MvvmScreen(viewModel) { _ in Text("filter") })

        viewModel.onSelect(status: "cancelled")
        let shown = Date()
        XCTAssertEqual(viewModel.chrome.value.messages.map(\.durationMillis), [4000])

        waitUntil("the message expired", timeout: 10) { viewModel.chrome.value.messages.isEmpty }
        XCTAssertGreaterThanOrEqual(Date().timeIntervalSince(shown), 3.9)
        window.isHidden = true
    }

    private func loadedOrders() -> OrdersViewModel {
        let viewModel = OrdersViewModel(repo: OrdersRepository(), navigator: newNavigator())
        viewModel.load()
        waitUntil("first load fails") { viewModel.chrome.value.error != nil }
        viewModel.retry()
        waitUntil("orders loaded") { viewModel.state.value.orders.count == 3 }
        return viewModel
    }

    func testNavigatorStackPushesAndReportsSwipeBack() {
        let navigator = newNavigator()
        let window = host(NavigatorStack(navigator) { (route: OrdersRoute) in Text(String(describing: route)) })
        let controller = find(UINavigationController.self, in: window.rootViewController)
        XCTAssertEqual(controller?.viewControllers.count, 1)

        navigator.push(route: OrdersRoute.details(id: 1).kotlinValue)
        waitUntil("pushed") { controller?.viewControllers.count == 2 }

        navigator.onSystemPop(newPath: [])
        XCTAssertEqual(navigator.stack.value.path.count, 0)
        waitUntil("popped") { controller?.viewControllers.count == 1 }
        window.isHidden = true
    }

    func testSheetPresentationFollowsTheNavigator() {
        let navigator = newNavigator()
        let window = host(NavigatorStack(navigator) { (route: OrdersRoute) in
            switch route {
            case .filter(let preset): OrderFilterSheet(preset: preset)
            default: Text(String(describing: route))
            }
        })
        let root = window.rootViewController
        let start = SampleGraph.shared.lifecycleLog.count

        navigator.push(route: OrdersRoute.filter(preset: nil).kotlinValue)
        waitUntil("sheet opened") { root?.presentedViewController != nil && self.log(since: start).count == 1 }
        XCTAssertEqual(log(since: start), ["OrderFilterViewModel created"])

        let afterOpen = SampleGraph.shared.lifecycleLog.count
        navigator.push(route: OrdersRoute.filter(preset: "paid").kotlinValue)
        waitUntil("sheet replaced") { self.log(since: afterOpen).count == 2 }
        XCTAssertNotNil(root?.presentedViewController)
        XCTAssertEqual(log(since: afterOpen).sorted(), ["OrderFilterViewModel cleared", "OrderFilterViewModel created"])

        let afterReplace = SampleGraph.shared.lifecycleLog.count
        navigator.push(route: OrdersRoute.filter(preset: "paid").kotlinValue)
        settle()
        XCTAssertEqual(log(since: afterReplace), [])

        navigator.push(route: OrdersRoute.details(id: 1).kotlinValue)
        waitUntil("sheet closed") { root?.presentedViewController == nil && self.log(since: afterReplace).count == 1 }
        XCTAssertEqual(log(since: afterReplace), ["OrderFilterViewModel cleared"])
        XCTAssertEqual(navigator.stack.value.path.count, 1)

        navigator.onSystemDismissSheet(expected: OrdersRoute.filter(preset: "paid").kotlinValue)
        XCTAssertEqual(navigator.stack.value.path.count, 1)
        window.isHidden = true
    }
}
