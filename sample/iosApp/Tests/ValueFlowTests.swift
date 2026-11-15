import SampleShared
import XCTest

@MainActor
final class ValueFlowTests: XCTestCase {

    private func waitUntil(_ description: String, _ condition: () -> Bool) {
        let deadline = Date().addingTimeInterval(5)
        while !condition() && Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.05))
        }
        XCTAssertTrue(condition(), description)
    }

    func testNumbersTextsAndEnumsAreReadAsSwiftValues() {
        let flows = ValueFlows()

        let count: Int32 = flows.count.value
        XCTAssertEqual(count, 1)
        XCTAssertNil(flows.total.value)
        XCTAssertTrue(flows.enabled.value)
        XCTAssertEqual(flows.title.value, "Hello")
        XCTAssertNil(flows.maybeTitle.value)
        XCTAssertEqual(flows.label.value, "Label")
        XCTAssertEqual(flows.mode.value, .compact)
    }

    func testCollectionsAndObjectsAreReadAsSwiftValues() {
        let flows = ValueFlows()

        XCTAssertEqual(flows.tags.value, ["a", "b"])
        XCTAssertEqual(flows.scores.value.mapValues(\.intValue), ["a": 1])
        XCTAssertEqual(flows.addresses.value, [Address(city: "Oslo", zip: 150)])
        XCTAssertNil(flows.maybeAddresses.value)
        XCTAssertNil(flows.selected.value)
        let repository: OrdersRepository = flows.repository.value
        XCTAssertTrue(repository === flows.repository.value)
        XCTAssertNil(flows.holder.value.state)
    }

    func testAFlowOfValuesIsObservedAndCached() {
        let flows = ValueFlows()
        let count = flows.count
        let selected = flows.selected
        XCTAssertTrue(count === flows.count)

        flows.increment()
        flows.select(address: Address(city: "Rome", zip: 1).kotlinValue)

        waitUntil("the count follows Kotlin") { count.value == 2 }
        waitUntil("the nullable value follows Kotlin") { selected.value == Address(city: "Rome", zip: 1) }

        flows.select(address: nil)
        waitUntil("the value becomes nil again") { selected.value == nil }
    }

    func testEventsOfValuesArriveAsSwiftValues() async {
        let flows = ValueFlows()

        var clicks: [Int32] = []
        for await click in flows.clicks { clicks.append(click) }
        XCTAssertEqual(clicks, [1, 2, 3])

        var ticks = 0
        for await _ in flows.ticks { ticks += 1 }
        XCTAssertEqual(ticks, 2)
    }
}
