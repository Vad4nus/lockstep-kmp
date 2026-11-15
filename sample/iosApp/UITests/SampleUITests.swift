import XCTest

final class SampleUITests: XCTestCase {

    private func launchOrders() -> XCUIApplication {
        continueAfterFailure = false
        let app = XCUIApplication()
        app.launch()
        let retry = app.buttons["Try again"]
        XCTAssertTrue(retry.waitForExistence(timeout: 20), "the first load fails and shows the error view")
        retry.tap()
        XCTAssertTrue(app.staticTexts["Keyboard"].waitForExistence(timeout: 10))
        return app
    }

    private func deleteButton(_ app: XCUIApplication, row: Int) -> XCUIElement {
        app.buttons.matching(identifier: "Delete").element(boundBy: row)
    }

    func testASecondAlertOpenedFromTheFirstOneStaysAndBothReportTheirButton() {
        let app = launchOrders()

        deleteButton(app, row: 0).tap()
        XCTAssertTrue(app.alerts.staticTexts["Delete Keyboard?"].waitForExistence(timeout: 5))
        app.alerts.buttons["Delete"].tap()

        let second = app.alerts.staticTexts["Keyboard is paid. Delete it anyway?"]
        XCTAssertTrue(second.waitForExistence(timeout: 5))
        XCTAssertFalse(app.alerts.staticTexts["Delete Keyboard?"].exists)
        XCTAssertTrue(app.staticTexts["Keyboard"].exists, "the order stays until the second answer")
        app.alerts.buttons["Cancel"].tap()
        XCTAssertTrue(second.waitForNonExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Keyboard"].exists)

        deleteButton(app, row: 0).tap()
        XCTAssertTrue(app.alerts.staticTexts["Delete Keyboard?"].waitForExistence(timeout: 5))
        app.alerts.buttons["Delete"].tap()
        XCTAssertTrue(second.waitForExistence(timeout: 5))
        app.alerts.buttons["Delete"].tap()

        XCTAssertTrue(app.staticTexts["Order deleted"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Keyboard"].waitForNonExistence(timeout: 5))

        app.buttons["Undo"].tap()
        XCTAssertTrue(app.staticTexts["Keyboard"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Order deleted"].waitForNonExistence(timeout: 5))
    }

    func testPullToRefreshLoadsTheOrdersAgain() {
        let app = launchOrders()

        deleteButton(app, row: 1).tap()
        XCTAssertTrue(app.alerts.staticTexts["Delete Monitor?"].waitForExistence(timeout: 5))
        app.alerts.buttons["Delete"].tap()
        XCTAssertTrue(app.staticTexts["Monitor"].waitForNonExistence(timeout: 5))

        let list = app.collectionViews.firstMatch
        list.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2))
            .press(forDuration: 0.1, thenDragTo: list.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.9)))

        XCTAssertTrue(app.staticTexts["Monitor"].waitForExistence(timeout: 10))
    }
}
