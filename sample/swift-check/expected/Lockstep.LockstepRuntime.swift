import Foundation
import Observation

/// An observable copy of a Kotlin state flow. Every change maps the whole value on the main thread,
/// so keep a large part that changes rarely in a flow of its own.
@available(iOS 17.0, *)
public final class UiState<Value>: Observable {
    private let registrar = ObservationRegistrar()
    private var storage: Value
    private var subscription: UiSubscription?

    public var value: Value {
        registrar.access(self, keyPath: \.value)
        return storage
    }

    public init<Kotlin: AnyObject>(_ flow: UiStateFlow<Kotlin>, _ map: @escaping (Kotlin?) -> Value) {
        var last = flow.value
        storage = map(last)
        subscription = flow.subscribe(onValue: { [weak self] kotlin in
            if kotlin === last { return }
            last = kotlin
            self?.update(map(kotlin))
        })
    }

    private func update(_ newValue: Value) {
        registrar.withMutation(of: self, keyPath: \.value) {
            storage = newValue
        }
    }

    deinit {
        subscription?.cancel()
    }
}

/// The events of a Kotlin flow as an async sequence. Every iteration subscribes on its own, and a flow backed
/// by a channel hands each event to one subscriber only, so iterate such a flow in one place.
/// A flow that fails stops the sequence; debug builds also stop on an assertion.
@available(iOS 17.0, *)
public struct UiEvents<Value>: AsyncSequence {
    public typealias Element = Value

    private let makeStream: () -> AsyncStream<Value>

    public init<Kotlin: AnyObject>(_ flow: UiFlow<Kotlin>, _ map: @escaping (Kotlin?) -> Value) {
        makeStream = {
            AsyncStream { continuation in
                let subscription = flow.subscribe(
                    onValue: { continuation.yield(map($0)) },
                    onComplete: { failure in
                        if let failure { assertionFailure("lockstep: the flow failed: \(failure)") }
                        continuation.finish()
                    }
                )
                continuation.onTermination = { _ in subscription.cancel() }
            }
        }
    }

    public func makeAsyncIterator() -> AsyncStream<Value>.Iterator {
        makeStream().makeAsyncIterator()
    }
}
