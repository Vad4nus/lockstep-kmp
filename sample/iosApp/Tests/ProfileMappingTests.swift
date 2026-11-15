import Observation
import SampleShared
import XCTest

@MainActor
final class ProfileMappingTests: XCTestCase {

    private func wait(_ state: UiState<ProfileState>, until predicate: @escaping (ProfileState) -> Bool) {
        let expectation = expectation(description: "state")
        expectation.assertForOverFulfill = false
        func check() {
            if predicate(state.value) {
                expectation.fulfill()
                return
            }
            withObservationTracking({ _ = state.value }, onChange: {
                DispatchQueue.main.async { check() }
            })
        }
        check()
        wait(for: [expectation], timeout: 5)
    }

    func testObservationNotifiesOnEveryEmission() {
        let viewModel = ProfileViewModel()
        let state = viewModel.state
        var changes = 0

        withObservationTracking({ _ = state.value }, onChange: { changes += 1 })
        viewModel.onNameChanged(name: "A")
        XCTAssertEqual(changes, 1)

        withObservationTracking({ _ = state.value }, onChange: { changes += 1 })
        viewModel.onNameChanged(name: "B")
        XCTAssertEqual(changes, 2)
        XCTAssertEqual(state.value.name, "B")
    }

    func testInitialStateMapsAllFields() {
        let state = ProfileViewModel().state.value

        XCTAssertEqual(state.name, "")
        XCTAssertNil(state.age)
        XCTAssertNil(state.address)
        XCTAssertEqual(state.screen, .idle)
    }

    func testUpdatesProduceNativeValues() {
        let viewModel = ProfileViewModel()
        viewModel.onNameChanged(name: "Ann")
        viewModel.onAgeChanged(age: 30)
        viewModel.onAddressChanged(city: "Oslo", zip: 150)

        wait(viewModel.state) { $0.name == "Ann" && $0.age == 30 && $0.address != nil }

        let state = viewModel.state.value
        XCTAssertEqual(state.age, 30)
        XCTAssertEqual(state.address, Address(city: "Oslo", zip: 150))
    }

    func testStructKeepsKotlinObjectOnlyWhenMappedFromKotlin() {
        let mapped = ProfileViewModel().state.value
        XCTAssertNotNil(mapped.kotlin)

        let built = Address(city: "Oslo", zip: 150)
        XCTAssertNil(built.kotlin)
        XCTAssertEqual(built, Address(city: "Oslo", zip: 150))
    }

    func testProxiesCallKotlinFunctionsOnMappedStructs() {
        let address = Address(__Address(city: "Oslo", zip: 150))

        XCTAssertEqual(address.label(), "Oslo 150")
        XCTAssertEqual(address.isNear(other: address), true)
        XCTAssertEqual(address.zipOrNull(), 150)
        XCTAssertEqual(address.next()?.zip, 151)
        XCTAssertEqual(address.formatter()(), "Oslo/150")
        XCTAssertEqual(address.tag(kotlin: "x"), "x:Oslo")
        XCTAssertEqual(address.doCopy(prefix: "New ").city, "New Oslo")

        let before = AddressFunctionsKt.logCount
        address.log()
        XCTAssertEqual(AddressFunctionsKt.logCount, before + 1)

        let state = ProfileViewModel().state.value
        XCTAssertEqual(state.withName(name: "Ann").name, "Ann")
        XCTAssertEqual(state.screenOrIdle(), .idle)
    }

    func testProxiesRunOnStructsBuiltInSwift() {
        let built = Address(city: "Oslo", zip: 150)
        let mapped = Address(__Address(city: "Bergen", zip: 150))

        XCTAssertEqual(built.label(), "Oslo 150")
        XCTAssertEqual(built.zipOrNull(), 150)
        XCTAssertEqual(built.next(), Address(city: "Oslo", zip: 151))
        XCTAssertTrue(mapped.isNear(other: built))
        XCTAssertEqual(ProfileState(ProfileViewModel().state.value.kotlinValue).withName(name: "Ann").name, "Ann")

        let before = AddressFunctionsKt.logCount
        built.log()
        XCTAssertEqual(AddressFunctionsKt.logCount, before + 1)
    }

    func testLoadEventuallyGivesError() {
        let viewModel = ProfileViewModel()
        viewModel.onLoad()

        wait(viewModel.state) { $0.screen == .error(message: "Failed", code: 42) }

        XCTAssertEqual(viewModel.state.value.screen, .error(message: "Failed", code: 42))
    }

    func testActionsAndFiniteEvents() async {
        let viewModel = ProfileViewModel()
        viewModel.onBack()

        for await action in viewModel.actions {
            XCTAssertEqual(action, .navigateBack)
            break
        }

        var count = 0
        for await _ in viewModel.finiteEvents { count += 1 }
        XCTAssertEqual(count, 1)
    }

    func testStateIsCachedPerViewModelAndReleasedOnDeinit() {
        let viewModel = ProfileViewModel()
        XCTAssertTrue(viewModel.state === viewModel.state)
        XCTAssertFalse(viewModel.state === ProfileViewModel().state)

        var standalone: UiState<ProfileState>? = UiState(viewModel.__state) { (kotlin: __ProfileState?) -> ProfileState in
            ProfileState(kotlin!)
        }
        weak var released = standalone
        standalone = nil
        XCTAssertNil(released)

        viewModel.onNameChanged(name: "after release")
        XCTAssertEqual(viewModel.state.value.name, "after release")
    }
}
