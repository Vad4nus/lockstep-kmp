package io.github.vad4nus.lockstep.skie.render

import io.github.vad4nus.lockstep.skie.model.EnumCase
import io.github.vad4nus.lockstep.skie.model.Field
import io.github.vad4nus.lockstep.skie.model.FieldKind
import io.github.vad4nus.lockstep.skie.model.FlowAccessor
import io.github.vad4nus.lockstep.skie.model.FlowKind
import io.github.vad4nus.lockstep.skie.model.FlowValue
import io.github.vad4nus.lockstep.skie.model.MvvmModel
import io.github.vad4nus.lockstep.skie.model.ProxyExtension
import io.github.vad4nus.lockstep.skie.model.ProxyFunction
import io.github.vad4nus.lockstep.skie.model.ProxyParameter
import io.github.vad4nus.lockstep.skie.model.SwiftEnum
import io.github.vad4nus.lockstep.skie.model.SwiftStruct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwiftRendererTest {

    private val int32 = FieldKind.BoxedPrimitive("Int32", "int32Value", "KotlinInt(int:")

    @Test
    fun structWithPassthroughBoxedNestedAndKeywordFields() {
        val struct = SwiftStruct(
            name = "ProfileState",
            kotlinSwiftName = "__ProfileState",
            fields = listOf(
                Field("name", FieldKind.Passthrough("String")),
                Field("age", int32),
                Field("address", FieldKind.Generated("Address", nullable = true)),
                Field("default", FieldKind.Passthrough("String")),
            ),
            equatable = true,
        )

        assertEquals(
            """
            import Foundation

            public struct ProfileState: Equatable {
                public let name: String
                public let age: Int32?
                public let address: Address?
                public let `default`: String
                public let kotlin: __ProfileState?

                public init(name: String, age: Int32?, address: Address?, `default`: String) {
                    self.name = name
                    self.age = age
                    self.address = address
                    self.`default` = `default`
                    self.kotlin = nil
                }

                public init(_ kotlin: __ProfileState) {
                    self.name = kotlin.name
                    self.age = kotlin.age?.int32Value
                    self.address = kotlin.address.map(Address.init)
                    self.`default` = kotlin.`default`
                    self.kotlin = kotlin
                }

                public static func == (lhs: ProfileState, rhs: ProfileState) -> Bool {
                    lhs.name == rhs.name && lhs.age == rhs.age && lhs.address == rhs.address && lhs.`default` == rhs.`default`
                }
            }

            """.trimIndent(),
            render(listOf(struct), emptyList(), emptyList()),
        )
    }

    @Test
    fun sealedEnumWithObjectAndDataCases() {
        val enum = SwiftEnum(
            name = "ProfileScreenState",
            kotlinSwiftName = "__ProfileScreenState",
            cases = listOf(
                EnumCase("idle", "ProfileScreenStateIdle", fields = emptyList()),
                EnumCase(
                    "error",
                    "ProfileScreenStateError",
                    fields = listOf(
                        Field("message", FieldKind.Passthrough("String")),
                        Field("code", int32),
                    ),
                ),
            ),
            equatable = true,
        )

        assertEquals(
            """
            import Foundation

            public enum ProfileScreenState: Equatable {
                case idle
                case error(message: String, code: Int32?)

                public init(_ kotlin: __ProfileScreenState) {
                    if kotlin is ProfileScreenStateIdle { self = .idle; return }
                    if let c = kotlin as? ProfileScreenStateError { self = .error(message: c.message, code: c.code?.int32Value); return }
                    fatalError("Unknown subclass of ProfileScreenState")
                }
            }

            """.trimIndent(),
            render(emptyList(), listOf(enum), emptyList()),
        )
    }

    @Test
    fun recursiveEnumIsIndirectAndMapsChildrenNatively() {
        val enum = SwiftEnum(
            name = "Node",
            kotlinSwiftName = "__Node",
            cases = listOf(
                EnumCase("card", "NodeCard", fields = listOf(Field("content", FieldKind.Generated("Node", nullable = false)))),
                EnumCase("column", "NodeColumn", fields = listOf(Field("children", FieldKind.GeneratedList("Node", nullable = false)))),
            ),
            equatable = true,
            indirect = true,
        )

        assertEquals(
            """
            import Foundation

            public indirect enum Node: Equatable {
                case card(content: Node)
                case column(children: [Node])

                public init(_ kotlin: __Node) {
                    if let c = kotlin as? NodeCard { self = .card(content: Node(c.content)); return }
                    if let c = kotlin as? NodeColumn { self = .column(children: c.children.map(Node.init)); return }
                    fatalError("Unknown subclass of Node")
                }
            }

            """.trimIndent(),
            render(emptyList(), listOf(enum), emptyList()),
        )
    }

    @Test
    fun nonEquatableStructOmitsConformance() {
        val struct = SwiftStruct(
            name = "Wrapper",
            kotlinSwiftName = "Wrapper",
            fields = listOf(Field("inner", FieldKind.Generated("Callback", nullable = false))),
            equatable = false,
        )

        val output = render(listOf(struct), emptyList(), emptyList())

        assertTrue("public struct Wrapper {" in output)
        assertFalse("Equatable" in output)
        assertTrue("self.inner = Callback(kotlin.inner)" in output)
        assertTrue("public let kotlin: Wrapper?" in output)
        assertFalse("==" in output)
    }

    @Test
    fun equatableStructWithoutFieldsComparesAsEqual() {
        val struct = SwiftStruct(name = "Empty", kotlinSwiftName = "__Empty", fields = emptyList(), equatable = true)

        assertEquals(
            """
            import Foundation

            public struct Empty: Equatable {
                public let kotlin: __Empty?

                public init(_ kotlin: __Empty) {
                    self.kotlin = kotlin
                }

                public static func == (lhs: Empty, rhs: Empty) -> Bool {
                    true
                }
            }

            """.trimIndent(),
            render(listOf(struct), emptyList(), emptyList()),
        )
    }

    @Test
    fun proxiesGuardTheKotlinObjectAndConvertResults() {
        val string = FieldKind.Passthrough("Swift.String?")
        val extension = ProxyExtension(
            "Address",
            listOf(
                ProxyFunction("log", emptyList(), null, "log()"),
                ProxyFunction("label", emptyList(), string, "label()"),
                ProxyFunction(
                    "isNear",
                    listOf(ProxyParameter(null, "other", "Address", kotlinBinding = "lockstepArg0")),
                    FieldKind.Passthrough("Swift.Bool?"),
                    "isNear(other: lockstepArg0)",
                ),
                ProxyFunction("next", emptyList(), FieldKind.Generated("Address", nullable = true), "next()"),
                ProxyFunction("self_", emptyList(), FieldKind.Generated("Address", nullable = false), "self_()"),
                ProxyFunction("zip", emptyList(), int32, "zip()"),
                ProxyFunction(
                    "formatter",
                    emptyList(),
                    FieldKind.Passthrough("(() -> Swift.String)?"),
                    "formatter()",
                ),
                ProxyFunction(
                    "tag",
                    listOf(
                        ProxyParameter(null, "kotlin", "Swift.String", kotlinBinding = null),
                        ProxyParameter("_", "default", "Swift.String", kotlinBinding = null),
                        ProxyParameter("with", "suffix", "Swift.String", kotlinBinding = null),
                    ),
                    string,
                    "tag(kotlin: kotlin, `default`, with: suffix)",
                ),
            ),
        )

        assertEquals(
            """
            import Foundation

            extension Address {
                public func log() {
                    guard let lockstepSelf = self.kotlin else { return }
                    lockstepSelf.log()
                }

                public func label() -> Swift.String? {
                    guard let lockstepSelf = self.kotlin else { return nil }
                    return lockstepSelf.label()
                }

                public func isNear(other: Address) -> Swift.Bool? {
                    guard let lockstepSelf = self.kotlin, let lockstepArg0 = other.kotlin else { return nil }
                    return lockstepSelf.isNear(other: lockstepArg0)
                }

                public func next() -> Address? {
                    guard let lockstepSelf = self.kotlin else { return nil }
                    return lockstepSelf.next().map(Address.init)
                }

                public func self_() -> Address? {
                    guard let lockstepSelf = self.kotlin else { return nil }
                    return Address(lockstepSelf.self_())
                }

                public func zip() -> Int32? {
                    guard let lockstepSelf = self.kotlin else { return nil }
                    return lockstepSelf.zip()?.int32Value
                }

                public func formatter() -> (() -> Swift.String)? {
                    guard let lockstepSelf = self.kotlin else { return nil }
                    return lockstepSelf.formatter()
                }

                public func tag(kotlin: Swift.String, _ `default`: Swift.String, with suffix: Swift.String) -> Swift.String? {
                    guard let lockstepSelf = self.kotlin else { return nil }
                    return lockstepSelf.tag(kotlin: kotlin, `default`, with: suffix)
                }
            }

            """.trimIndent(),
            renderProxies(listOf(extension)),
        )
    }

    @Test
    fun stateAndEventsAccessors() {
        val state = FlowAccessor("ProfileViewModel", "state", "__state", FlowKind.State, FlowValue.Generated("ProfileState"), valueNullable = false)
        val events = FlowAccessor("ProfileViewModel", "actions", "__actions", FlowKind.Events, FlowValue.Generated("ProfileAction"), valueNullable = true)

        assertEquals(
            """
            import Foundation

            private let LockstepKey_ProfileViewModel_state: StaticString = "LockstepKey_ProfileViewModel_state"

            @available(iOS 17.0, *)
            extension ProfileViewModel {
                public var state: UiState<ProfileState> {
                    if let cached = objc_getAssociatedObject(self, LockstepKey_ProfileViewModel_state.utf8Start) as? UiState<ProfileState> {
                        return cached
                    }
                    let created = UiState(__state, { kotlin in ProfileState(kotlin!) })
                    objc_setAssociatedObject(self, LockstepKey_ProfileViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                    return created
                }
            }

            @available(iOS 17.0, *)
            extension ProfileViewModel {
                public var actions: UiEvents<ProfileAction?> {
                    UiEvents(__actions, { kotlin in kotlin.map { kotlin in ProfileAction(kotlin) } })
                }
            }

            """.trimIndent(),
            render(emptyList(), emptyList(), listOf(state, events)),
        )
    }

    @Test
    fun flowsOfValuesThatAreNotGeneratedTypesReadThemTheWaySwiftBridgesThem() {
        fun events(name: String, value: FlowValue, nullable: Boolean = false) =
            render(emptyList(), emptyList(), listOf(FlowAccessor("Owner", name, "__$name", FlowKind.Events, value, nullable)))

        val count = events("count", FlowValue.Boxed("Int32", "int32Value"))
        assertTrue("public var count: UiEvents<Int32> {" in count)
        assertTrue("UiEvents(__count, { kotlin in kotlin!.int32Value })" in count)

        val total = events("total", FlowValue.Boxed("Int64", "int64Value"), nullable = true)
        assertTrue("public var total: UiEvents<Int64?> {" in total)
        assertTrue("UiEvents(__total, { kotlin in kotlin.map { kotlin in kotlin.int64Value } })" in total)

        val orders = events("orders", FlowValue.GeneratedList("Order", "__Order"))
        assertTrue("public var orders: UiEvents<[Order]> {" in orders)
        assertTrue("UiEvents(__orders, { kotlin in (kotlin! as! [__Order]).map { Order(\$0) } })" in orders)

        val title = events("title", FlowValue.Localized)
        assertTrue("public var title: UiEvents<Swift.String> {" in title)
        assertTrue("UiEvents(__title, { kotlin in kotlin!.localized() })" in title)

        val tags = events("tags", FlowValue.Bridged("[Swift.String]"), nullable = true)
        assertTrue("public var tags: UiEvents<[Swift.String]?> {" in tags)
        assertTrue("UiEvents(__tags, { kotlin in kotlin.map { kotlin in (kotlin as AnyObject) as! [Swift.String] } })" in tags)
    }

    @Test
    fun runtimeDeclaresObservableAndAsyncSequence() {
        val output = runtimeSwift("UiStateFlow", "UiFlow", "UiSubscription")

        assertTrue("ObservationRegistrar" in output)
        assertTrue("registrar.access(" in output)
        assertTrue("registrar.withMutation(" in output)
        assertTrue("@available(iOS 17.0, *)" in output)
        assertTrue("import Combine" !in output)
        assertTrue("ObservableObject" !in output)
        assertTrue("AsyncSequence" in output)
        assertTrue("UiStateFlow<Kotlin>" in output)
        assertTrue("UiFlow<Kotlin>" in output)
        assertTrue("if kotlin === last { return }" in output)
        assertTrue("if let failure { assertionFailure(" in output)
        assertTrue("continuation.yield(map(\$0))" in output)
    }

    @Test
    fun listAndLocalizedFields() {
        val struct = SwiftStruct(
            name = "OrdersState",
            kotlinSwiftName = "__OrdersState",
            fields = listOf(
                Field("items", FieldKind.GeneratedList("Order", nullable = false)),
                Field("filters", FieldKind.GeneratedList("Filter", nullable = true)),
                Field("title", FieldKind.Localized(nullable = false)),
                Field("hint", FieldKind.Localized(nullable = true)),
            ),
            equatable = true,
        )

        assertEquals(
            """
            import Foundation

            public struct OrdersState: Equatable {
                public let items: [Order]
                public let filters: [Filter]?
                public let title: Swift.String
                public let hint: Swift.String?
                public let kotlin: __OrdersState?

                public init(items: [Order], filters: [Filter]?, title: Swift.String, hint: Swift.String?) {
                    self.items = items
                    self.filters = filters
                    self.title = title
                    self.hint = hint
                    self.kotlin = nil
                }

                public init(_ kotlin: __OrdersState) {
                    self.items = kotlin.items.map(Order.init)
                    self.filters = kotlin.filters.map { ${'$'}0.map(Filter.init) }
                    self.title = kotlin.title.localized()
                    self.hint = kotlin.hint?.localized()
                    self.kotlin = kotlin
                }

                public static func == (lhs: OrdersState, rhs: OrdersState) -> Bool {
                    lhs.items == rhs.items && lhs.filters == rhs.filters && lhs.title == rhs.title && lhs.hint == rhs.hint
                }
            }

            """.trimIndent(),
            render(listOf(struct), emptyList(), emptyList()),
        )
    }

    @Test
    fun hashableStructHashesFieldsAndRebuildsTheKotlinObject() {
        val struct = SwiftStruct(
            name = "Address",
            kotlinSwiftName = "__Address",
            fields = listOf(
                Field("city", FieldKind.Passthrough("Swift.String")),
                Field("zip", int32),
                Field("owner", FieldKind.Generated("Person", nullable = true)),
            ),
            equatable = true,
            hashable = true,
            writable = true,
        )

        val output = render(listOf(struct), emptyList(), emptyList())

        assertTrue("public struct Address: Hashable {" in output)
        assertTrue("hasher.combine(city)\n        hasher.combine(zip)\n        hasher.combine(owner)" in output)
        assertTrue("kotlin ?? __Address(city: city, zip: zip.map { KotlinInt(int: ${'$'}0) }, owner: owner?.kotlinValue)" in output)
    }

    @Test
    fun kotlinValueUsesTheLabelsOfTheKotlinConstructor() {
        val struct = SwiftStruct(
            name = "Item",
            kotlinSwiftName = "__Item",
            fields = listOf(
                Field("description_", FieldKind.Passthrough("Swift.String"), constructorLabel = "description"),
                Field("title", FieldKind.Passthrough("Swift.String"), constructorLabel = "_"),
                Field("summary", FieldKind.Passthrough("Swift.String"), constructorLabel = null),
            ),
            equatable = true,
            writable = true,
        )

        val output = render(listOf(struct), emptyList(), emptyList())

        assertTrue("public init(description_: Swift.String, title: Swift.String, summary: Swift.String) {" in output)
        assertTrue("kotlin ?? __Item(description: description_, title)" in output)
    }

    @Test
    fun listsAndTextsAreWrittenBackToKotlin() {
        val raw = "RawStringDesc.init(string:)"
        val struct = SwiftStruct(
            name = "OrdersState",
            kotlinSwiftName = "__OrdersState",
            fields = listOf(
                Field("items", FieldKind.GeneratedList("Order", nullable = false)),
                Field("filters", FieldKind.GeneratedList("Filter", nullable = true)),
                Field("title", FieldKind.Localized(nullable = false, raw)),
                Field("hint", FieldKind.Localized(nullable = true, raw)),
            ),
            equatable = true,
            writable = true,
        )

        val output = render(listOf(struct), emptyList(), emptyList())

        assertTrue(
            "kotlin ?? __OrdersState(items: items.map { ${'$'}0.kotlinValue }, filters: filters?.map { ${'$'}0.kotlinValue }, " +
                "title: RawStringDesc.init(string:)(title), hint: hint.map(RawStringDesc.init(string:)))" in output,
        )
    }

    @Test
    fun proxiesOfAWritableStructRunWithoutAKotlinObject() {
        val writable = ProxyParameter(null, "other", "Address", kotlinBinding = "lockstepArg0", writable = true)
        val readOnly = ProxyParameter(null, "area", "Area", kotlinBinding = "lockstepArg0")
        val extension = ProxyExtension(
            "Address",
            listOf(
                ProxyFunction("log", emptyList(), null, "log()", total = true),
                ProxyFunction("isNear", listOf(writable), FieldKind.Passthrough("Swift.Bool"), "isNear(other: lockstepArg0)", total = true),
                ProxyFunction("next", emptyList(), FieldKind.Generated("Address", nullable = false), "next()", total = true),
                ProxyFunction("isIn", listOf(readOnly), FieldKind.Passthrough("Swift.Bool?"), "isIn(area: lockstepArg0)"),
            ),
            writable = true,
        )

        assertEquals(
            """
            import Foundation

            extension Address {
                public func log() {
                    let lockstepSelf = self.kotlinValue
                    lockstepSelf.log()
                }

                public func isNear(other: Address) -> Swift.Bool {
                    let lockstepSelf = self.kotlinValue
                    let lockstepArg0 = other.kotlinValue
                    return lockstepSelf.isNear(other: lockstepArg0)
                }

                public func next() -> Address {
                    let lockstepSelf = self.kotlinValue
                    return Address(lockstepSelf.next())
                }

                public func isIn(area: Area) -> Swift.Bool? {
                    let lockstepSelf = self.kotlinValue
                    guard let lockstepArg0 = area.kotlin else { return nil }
                    return lockstepSelf.isIn(area: lockstepArg0)
                }
            }

            """.trimIndent(),
            renderProxies(listOf(extension)),
        )
    }

    @Test
    fun aKotlinToStringBecomesTheDescriptionOfTheStruct() {
        val describe = ProxyFunction("description", emptyList(), FieldKind.Passthrough("Swift.String"), "description()", total = true, description = true)
        val clear = ProxyFunction("clear", emptyList(), null, "clear()", total = true)

        assertEquals(
            """
            import Foundation

            extension FormState {
                public func clear() {
                    let lockstepSelf = self.kotlinValue
                    lockstepSelf.clear()
                }
            }

            extension FormState: CustomStringConvertible {
                public var description: Swift.String {
                    kotlinValue.description()
                }
            }

            """.trimIndent(),
            renderProxies(listOf(ProxyExtension("FormState", listOf(describe, clear), writable = true))),
        )
        assertEquals(
            """
            import Foundation

            extension FormState: CustomStringConvertible {
                public var description: Swift.String {
                    kotlinValue.description()
                }
            }

            """.trimIndent(),
            renderProxies(listOf(ProxyExtension("FormState", listOf(describe), writable = true))),
        )
    }

    @Test
    fun hashableRouteEnumRebuildsKotlinAndConvertsFromTheRouteProtocol() {
        val enum = SwiftEnum(
            name = "FixtureRoute",
            kotlinSwiftName = "__FixtureRoute",
            cases = listOf(
                EnumCase("home", "FixtureRouteHome", fields = emptyList(), isObject = true),
                EnumCase("item", "FixtureRouteItem", fields = listOf(Field("payload", FieldKind.Generated("Address", nullable = false)))),
                EnumCase("sheet", "FixtureRouteSheet", fields = listOf(Field("id", FieldKind.Passthrough("Swift.Int32")))),
            ),
            equatable = true,
            hashable = true,
            routeSwiftName = "Route",
            writable = true,
        )

        assertEquals(
            """
            import Foundation

            public enum FixtureRoute: Hashable {
                case home
                case item(payload: Address)
                case sheet(id: Swift.Int32)

                public init(_ kotlin: __FixtureRoute) {
                    if kotlin is FixtureRouteHome { self = .home; return }
                    if let c = kotlin as? FixtureRouteItem { self = .item(payload: Address(c.payload)); return }
                    if let c = kotlin as? FixtureRouteSheet { self = .sheet(id: c.id); return }
                    fatalError("Unknown subclass of FixtureRoute")
                }

                public init?(route: Route) {
                    guard let kotlin = route as? __FixtureRoute else { return nil }
                    self.init(kotlin)
                }

                public var kotlinValue: __FixtureRoute {
                    switch self {
                    case .home: return FixtureRouteHome.shared
                    case .item(let payload): return FixtureRouteItem(payload: payload.kotlinValue)
                    case .sheet(let id): return FixtureRouteSheet(id: id)
                    }
                }
            }

            """.trimIndent(),
            render(emptyList(), listOf(enum), emptyList()),
        )
    }

    @Test
    fun hashableEnumWithoutRouteOmitsTheRouteInitializer() {
        val enum = SwiftEnum(
            name = "Kind",
            kotlinSwiftName = "__Kind",
            cases = listOf(EnumCase("a", "KindA", fields = emptyList(), isObject = true)),
            equatable = true,
            hashable = true,
            writable = true,
        )

        val output = render(emptyList(), listOf(enum), emptyList())

        assertFalse("init?(route:" in output)
        assertTrue("public var kotlinValue: __Kind" in output)
    }

    @Test
    fun fieldsSharedByEveryCaseBecomePropertiesAndWritableEnumsRebuildKotlin() {
        val id = Field("id", FieldKind.Passthrough("Swift.String"))
        val enum = SwiftEnum(
            name = "Node",
            kotlinSwiftName = "__Node",
            cases = listOf(
                EnumCase("text", "NodeText", fields = listOf(id, Field("text", FieldKind.Passthrough("Swift.String")))),
                EnumCase("box", "NodeBox", fields = listOf(Field("text", FieldKind.Passthrough("Swift.String")), id)),
            ),
            equatable = true,
            writable = true,
        )

        val output = render(emptyList(), listOf(enum), emptyList())

        assertTrue(
            """
                public var id: Swift.String {
                    switch self {
                    case .text(let id, _): return id
                    case .box(_, let id): return id
                    }
                }
            """.trimIndent().prependIndent("    ") in output,
        )
        assertFalse("public var text:" in output)
        assertFalse("Hashable" in output)
        assertTrue("case .text(let id, let text): return NodeText(id: id, text: text)" in output)
    }

    private val mvvm = MvvmModel(
        androidViewModel = "AndroidxViewModel",
        viewModelStore = "ViewModelStore",
        navigator = "StackNavigator",
        route = "Route",
        chrome = "ScreenChrome",
        uiError = "UiError",
        uiMessage = "UiMessage",
        navStack = "NavStack",
        viewModels = listOf("OrdersViewModel"),
    )

    private val routeEnum = SwiftEnum(
        name = "FixtureRoute",
        kotlinSwiftName = "__FixtureRoute",
        cases = emptyList(),
        equatable = true,
        hashable = true,
        routeSwiftName = "Route",
    )

    @Test
    fun mvvmRuntimeConformsEveryViewModelToTheScreenModel() {
        val output = mvvmRuntimeSwift(mvvm, emptyList())

        assertTrue("public final class ViewModelOwner<VM: AndroidxViewModel>" in output)
        assertTrue("public protocol MvvmScreenModel: AnyObject {" in output)
        assertTrue("var chrome: UiState<ScreenChrome> { get }" in output)
        assertTrue("public struct MvvmScreen<VM: MvvmScreenModel, Content: View>: View {" in output)
        assertTrue("@available(iOS 17.0, *)\nextension OrdersViewModel: MvvmScreenModel {}" in output)
        assertTrue("struct NavigatorStack<R: NavigationRoute" in output)
        assertFalse("MvvmHooks" in output)
    }

    @Test
    fun mvvmRuntimeDrawsLoadingAndErrorOnTheirOwnAndReportsTheSystemPath() {
        val output = mvvmRuntimeSwift(mvvm, emptyList())

        assertTrue("if let error = current.error {" in output)
        assertTrue("if current.loading == .blocking {" in output)
        assertTrue("if let retry, let label = error.retryLabel {" in output)
        assertFalse("Try again" in output)
        assertFalse(".status" in output)
        assertTrue("navigator.onSystemPop(newPath: newPath.map { ${'$'}0.kotlinRoute })" in output)
    }

    @Test
    fun screensRuntimeMatchesTheNodeCasesAndNeverSendsAnUnknownEvent() {
        val output = screensRuntimeSwift()

        assertTrue("case .column(_, let children, let spacing, let align, let scroll, _, _, _):" in output)
        assertTrue("case .box(_, let children, _, _, _):" in output)
        assertTrue("case .unknown(_, let fallback, _, _, _):" in output)
        assertTrue("guard let event, event.known else { return nil }" in output)
        assertTrue("Button(label) { onEvent(event) }.disabled(!event.known)" in output)
    }

    @Test
    fun mvvmRuntimeConformsRouteEnumsToNavigationRoute() {
        val output = mvvmRuntimeSwift(mvvm, listOf(routeEnum))

        assertTrue(
            "extension FixtureRoute: NavigationRoute {\n    public var kotlinRoute: Route { kotlinValue }\n}" in output,
        )
    }
}
