import SampleShared
import XCTest

@MainActor
final class MappingFixtureTests: XCTestCase {

    func testListMapsToSwiftStructs() {
        let state = MappingFixtureViewModel().state.value

        XCTAssertEqual(state.addresses, [Address(city: "Oslo", zip: 150)])
        XCTAssertNil(state.maybeAddresses)
    }

    func testStringDescMapsToItsString() {
        let viewModel = MappingFixtureViewModel()
        XCTAssertEqual(viewModel.state.value.title, "Hello")
        XCTAssertNil(viewModel.state.value.maybeTitle)

        viewModel.onTitle(title: "Changed")
        XCTAssertEqual(viewModel.state.value.title, "Changed")
    }

    func testListsAndTextsAreWrittenBackToKotlin() {
        let built = MappingFixtureState(addresses: [Address(city: "Rome", zip: 1)], maybeAddresses: [], title: "Built", maybeTitle: "Maybe")
        XCTAssertNil(built.kotlin)

        let kotlin: __MappingFixtureState = built.kotlinValue
        XCTAssertEqual(kotlin.addresses.map(\.city), ["Rome"])
        XCTAssertEqual(MappingFixtureState(kotlin), built)
    }

    func testAFlowOfAnObjectGetsAnAccessorToo() {
        let state: UiState<MappingFixtureState> = FixtureNavigation.shared.state

        XCTAssertEqual(state.value.title, "Hello")
        XCTAssertTrue(state === FixtureNavigation.shared.state)
    }

    func testRouteRoundTripsBetweenSwiftAndKotlin() {
        let routes: [FixtureRoute] = [.home, .item(payload: Address(city: "Oslo", zip: 150)), .sheet(id: 7)]

        for route in routes {
            XCTAssertEqual(FixtureRoute(route: route.kotlinValue), route)
        }
    }

    func testRouteWorksAsSetElement() {
        let set: Set<FixtureRoute> = [.home, .sheet(id: 1), .sheet(id: 1), .sheet(id: 2)]

        XCTAssertEqual(set.count, 3)
        XCTAssertTrue(set.contains(.sheet(id: 2)))
    }

    func testRouteConversionMapsKotlinRoutesAndRejectsForeignOnes() {
        XCTAssertEqual(FixtureRoute(route: FixtureRouteSheet(id: 1)), .sheet(id: 1))
        XCTAssertNil(FixtureRoute(route: ForeignRoute.shared))
    }

    func testNavigatorStackConvertsThroughTheRealNavigator() {
        let navigator = FixtureNavigation.shared.navigator
        navigator.replaceAll(root: FixtureRoute.home.kotlinValue, path: [])
        XCTAssertNil(navigator.stack.value.sheet)

        navigator.push(route: FixtureRoute.sheet(id: 1).kotlinValue)
        XCTAssertEqual(navigator.stack.value.sheet.flatMap(FixtureRoute.init(route:)), .sheet(id: 1))

        navigator.onSystemDismissSheet(expected: FixtureRoute.sheet(id: 1).kotlinValue)
        XCTAssertNil(navigator.stack.value.sheet)
    }
}
