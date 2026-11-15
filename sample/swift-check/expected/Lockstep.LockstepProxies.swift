import Foundation

extension Address {
    public func doCopy(prefix: Swift.String) -> Address {
        let lockstepSelf = self.kotlinValue
        return Address(lockstepSelf.doCopy(prefix: prefix))
    }

    public func formatter() -> () -> Swift.String {
        let lockstepSelf = self.kotlinValue
        return lockstepSelf.formatter()
    }

    public func isNear(other: Address) -> Swift.Bool {
        let lockstepSelf = self.kotlinValue
        let lockstepArg0 = other.kotlinValue
        return lockstepSelf.isNear(other: lockstepArg0)
    }

    public func label() -> Swift.String {
        let lockstepSelf = self.kotlinValue
        return lockstepSelf.label()
    }

    public func log() {
        let lockstepSelf = self.kotlinValue
        lockstepSelf.log()
    }

    public func next() -> Address? {
        let lockstepSelf = self.kotlinValue
        return lockstepSelf.next().map(Address.init)
    }

    public func tag(kotlin: Swift.String) -> Swift.String {
        let lockstepSelf = self.kotlinValue
        return lockstepSelf.tag(kotlin: kotlin)
    }

    public func zipOrNull() -> Int32? {
        let lockstepSelf = self.kotlinValue
        return lockstepSelf.zipOrNull()?.int32Value
    }
}

extension ProfileState {
    public func screenOrIdle() -> ProfileScreenState {
        let lockstepSelf = self.kotlinValue
        return ProfileScreenState(lockstepSelf.screenOrIdle())
    }

    public func withName(name: Swift.String) -> ProfileState {
        let lockstepSelf = self.kotlinValue
        return ProfileState(lockstepSelf.withName(name: name))
    }
}

extension ScreenSpec {
    public func checked(onViolation: @escaping (SampleShared.LayoutViolation) -> Swift.Void) -> ScreenSpec {
        let lockstepSelf = self.kotlinValue
        return ScreenSpec(lockstepSelf.checked(onViolation: onViolation))
    }
}

extension FormState: CustomStringConvertible {
    public var description: Swift.String {
        kotlinValue.description()
    }
}
