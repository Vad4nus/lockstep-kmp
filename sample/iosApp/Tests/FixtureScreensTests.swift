import SampleShared
import SwiftUI
import XCTest

@MainActor
final class FixtureScreensTests: XCTestCase {

    private func fixture(_ name: String) -> FixtureViewModel {
        FixtureViewModel(name: name, navigator: StackNavigator(root: OrdersRoute.list.kotlinValue))
    }

    func testEveryFixtureRendersThroughNodeView() {
        for name in ["login", "feed", "update"] {
            let state = fixture(name).state.value

            XCTAssertEqual(state.screen.id, name)
            XCTAssertNotNil(ImageRenderer(content: NodeView(state.screen.root, form: state.form) { _ in }.frame(width: 390, height: 844)).uiImage, name)
        }
    }

    func testUnknownNodeTypesKeepTheirFallback() {
        guard case let .column(_, children, _, _, _, _, _, _) = fixture("update").state.value.screen.root else { return XCTFail("root is not a column") }

        let fallback = Node.text(id: "fallback", text: "This version cannot show a carousel. Update the app.", textStyle: .body, color: .secondary, style: nil, a11y: nil)
        XCTAssertEqual(children[1], .unknown(id: "promo", fallback: fallback, style: nil, a11y: nil, error: nil))
        XCTAssertEqual(children[2], .unknown(id: "silent", fallback: nil, style: nil, a11y: nil, error: nil))
        XCTAssertEqual(children.map(\.id), ["title", "promo", "silent", "badge", "login"])
    }

    func testAnUnknownFixtureNameGetsAScreenThatSaysSo() {
        let state = fixture("nope").state.value

        XCTAssertEqual(state.screen.id, "missing")
        XCTAssertNotNil(ImageRenderer(content: NodeView(state.screen.root, form: state.form) { _ in }.frame(width: 390, height: 844)).uiImage)
    }

    func testTheFormIsPrintedWithoutItsValues() {
        let form = FormState(values: ["password": "secret"], errors: [:])

        XCTAssertFalse(String(describing: form).contains("secret"))
        XCTAssertTrue(String(describing: form).contains("password"))
        XCTAssertFalse("\(FixtureState(screen: fixture("login").state.value.screen, form: form))".contains("secret"))
    }

    func testSwiftEventsReachTheViewModelAsKotlinValues() {
        let viewModel = fixture("login")

        viewModel.onEvent(event: NodeEvent.valueChanged(field: "email", value: "nope").kotlinValue)
        viewModel.onEvent(event: NodeEvent.submit.kotlinValue)

        let expected = FormState(values: ["email": "nope"], errors: ["email": "Not an email", "password": "At least 6 characters"])
        let deadline = Date().addingTimeInterval(5)
        while viewModel.state.value.form != expected && Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.05))
        }
        XCTAssertEqual(viewModel.state.value.form, expected)
    }
}
