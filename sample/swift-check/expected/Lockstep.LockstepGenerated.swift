import Foundation

public struct NavStack {
    public let root: SampleShared.Route
    public let path: [SampleShared.Route]
    public let sheet: SampleShared.Route?
    public let kotlin: __NavStack?

    public init(root: SampleShared.Route, path: [SampleShared.Route], sheet: SampleShared.Route?) {
        self.root = root
        self.path = path
        self.sheet = sheet
        self.kotlin = nil
    }

    public init(_ kotlin: __NavStack) {
        self.root = kotlin.root
        self.path = kotlin.path
        self.sheet = kotlin.sheet
        self.kotlin = kotlin
    }

    public var kotlinValue: __NavStack {
        kotlin ?? __NavStack(root: root, path: path, sheet: sheet)
    }
}

public struct Address: Hashable {
    public let city: Swift.String
    public let zip: Swift.Int32
    public let kotlin: __Address?

    public init(city: Swift.String, zip: Swift.Int32) {
        self.city = city
        self.zip = zip
        self.kotlin = nil
    }

    public init(_ kotlin: __Address) {
        self.city = kotlin.city
        self.zip = kotlin.zip
        self.kotlin = kotlin
    }

    public static func == (lhs: Address, rhs: Address) -> Bool {
        lhs.city == rhs.city && lhs.zip == rhs.zip
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(city)
        hasher.combine(zip)
    }

    public var kotlinValue: __Address {
        kotlin ?? __Address(city: city, zip: zip)
    }
}

public struct MappingFixtureState: Equatable {
    public let addresses: [Address]
    public let maybeAddresses: [Address]?
    public let title: Swift.String
    public let maybeTitle: Swift.String?
    public let kotlin: __MappingFixtureState?

    public init(addresses: [Address], maybeAddresses: [Address]?, title: Swift.String, maybeTitle: Swift.String?) {
        self.addresses = addresses
        self.maybeAddresses = maybeAddresses
        self.title = title
        self.maybeTitle = maybeTitle
        self.kotlin = nil
    }

    public init(_ kotlin: __MappingFixtureState) {
        self.addresses = kotlin.addresses.map(Address.init)
        self.maybeAddresses = kotlin.maybeAddresses.map { $0.map(Address.init) }
        self.title = kotlin.title.localized()
        self.maybeTitle = kotlin.maybeTitle?.localized()
        self.kotlin = kotlin
    }

    public static func == (lhs: MappingFixtureState, rhs: MappingFixtureState) -> Bool {
        lhs.addresses == rhs.addresses && lhs.maybeAddresses == rhs.maybeAddresses && lhs.title == rhs.title && lhs.maybeTitle == rhs.maybeTitle
    }

    public var kotlinValue: __MappingFixtureState {
        kotlin ?? __MappingFixtureState(addresses: addresses.map { $0.kotlinValue }, maybeAddresses: maybeAddresses?.map { $0.kotlinValue }, title: RawStringDesc.init(string:)(title), maybeTitle: maybeTitle.map(RawStringDesc.init(string:)))
    }
}

public struct ListState: Equatable {
    public let items: [Swift.String]
    public let kotlin: __ListState?

    public init(items: [Swift.String]) {
        self.items = items
        self.kotlin = nil
    }

    public init(_ kotlin: __ListState) {
        self.items = kotlin.items
        self.kotlin = kotlin
    }

    public static func == (lhs: ListState, rhs: ListState) -> Bool {
        lhs.items == rhs.items
    }

    public var kotlinValue: __ListState {
        kotlin ?? __ListState(items: items)
    }
}

public struct ProfileState: Equatable {
    public let name: Swift.String
    public let age: Int32?
    public let followers: Swift.Int64
    public let rating: Double?
    public let verified: Swift.Bool
    public let address: Address?
    public let tags: [Swift.String]
    public let screen: ProfileScreenState
    public let category: SampleShared.Category
    public let categoryPairs: [SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.__Category>]
    public let categories: [SampleShared.Category]
    public let categoryMap: [Swift.String : SampleShared.Category]
    public let maybeCategory: SampleShared.Category?
    public let pairOrNull: SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.__Category>?
    public let kotlin: __ProfileState?

    public init(name: Swift.String, age: Int32?, followers: Swift.Int64, rating: Double?, verified: Swift.Bool, address: Address?, tags: [Swift.String], screen: ProfileScreenState, category: SampleShared.Category, categoryPairs: [SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.__Category>], categories: [SampleShared.Category], categoryMap: [Swift.String : SampleShared.Category], maybeCategory: SampleShared.Category?, pairOrNull: SampleShared.KotlinPair<SampleShared.KotlinInt, SampleShared.__Category>?) {
        self.name = name
        self.age = age
        self.followers = followers
        self.rating = rating
        self.verified = verified
        self.address = address
        self.tags = tags
        self.screen = screen
        self.category = category
        self.categoryPairs = categoryPairs
        self.categories = categories
        self.categoryMap = categoryMap
        self.maybeCategory = maybeCategory
        self.pairOrNull = pairOrNull
        self.kotlin = nil
    }

    public init(_ kotlin: __ProfileState) {
        self.name = kotlin.name
        self.age = kotlin.age?.int32Value
        self.followers = kotlin.followers
        self.rating = kotlin.rating?.doubleValue
        self.verified = kotlin.verified
        self.address = kotlin.address.map(Address.init)
        self.tags = kotlin.tags
        self.screen = ProfileScreenState(kotlin.screen)
        self.category = kotlin.category
        self.categoryPairs = kotlin.categoryPairs
        self.categories = kotlin.categories
        self.categoryMap = kotlin.categoryMap
        self.maybeCategory = kotlin.maybeCategory
        self.pairOrNull = kotlin.pairOrNull
        self.kotlin = kotlin
    }

    public static func == (lhs: ProfileState, rhs: ProfileState) -> Bool {
        lhs.name == rhs.name && lhs.age == rhs.age && lhs.followers == rhs.followers && lhs.rating == rhs.rating && lhs.verified == rhs.verified && lhs.address == rhs.address && lhs.tags == rhs.tags && lhs.screen == rhs.screen && lhs.category == rhs.category && lhs.categoryPairs == rhs.categoryPairs && lhs.categories == rhs.categories && lhs.categoryMap == rhs.categoryMap && lhs.maybeCategory == rhs.maybeCategory && lhs.pairOrNull == rhs.pairOrNull
    }

    public var kotlinValue: __ProfileState {
        kotlin ?? __ProfileState(name: name, age: age.map { KotlinInt(int: $0) }, followers: followers, rating: rating.map { KotlinDouble(double: $0) }, verified: verified, address: address?.kotlinValue, tags: tags, screen: screen.kotlinValue, category: category, categoryPairs: categoryPairs, categories: categories, categoryMap: categoryMap, maybeCategory: maybeCategory, pairOrNull: pairOrNull)
    }
}

public struct SettingsState: Equatable {
    public let darkMode: Swift.Bool
    public let kotlin: __SettingsState?

    public init(darkMode: Swift.Bool) {
        self.darkMode = darkMode
        self.kotlin = nil
    }

    public init(_ kotlin: __SettingsState) {
        self.darkMode = kotlin.darkMode
        self.kotlin = kotlin
    }

    public static func == (lhs: SettingsState, rhs: SettingsState) -> Bool {
        lhs.darkMode == rhs.darkMode
    }

    public var kotlinValue: __SettingsState {
        kotlin ?? __SettingsState(darkMode: darkMode)
    }
}

public struct SpikeSection: Equatable {
    public let title: Swift.String
    public let body: SpikeNode
    public let subsections: [SpikeSection]
    public let kotlin: __SpikeSection?

    public init(title: Swift.String, body: SpikeNode, subsections: [SpikeSection]) {
        self.title = title
        self.body = body
        self.subsections = subsections
        self.kotlin = nil
    }

    public init(_ kotlin: __SpikeSection) {
        self.title = kotlin.title
        self.body = SpikeNode(kotlin.body)
        self.subsections = kotlin.subsections.map(SpikeSection.init)
        self.kotlin = kotlin
    }

    public static func == (lhs: SpikeSection, rhs: SpikeSection) -> Bool {
        lhs.title == rhs.title && lhs.body == rhs.body && lhs.subsections == rhs.subsections
    }

    public var kotlinValue: __SpikeSection {
        kotlin ?? __SpikeSection(title: title, body: body.kotlinValue, subsections: subsections.map { $0.kotlinValue })
    }
}

public struct SpikeLink: Equatable {
    public let label: Swift.String
    public let next: SampleShared.__SpikeLink?
    public let kotlin: __SpikeLink?

    public init(label: Swift.String, next: SampleShared.__SpikeLink?) {
        self.label = label
        self.next = next
        self.kotlin = nil
    }

    public init(_ kotlin: __SpikeLink) {
        self.label = kotlin.label
        self.next = kotlin.next
        self.kotlin = kotlin
    }

    public static func == (lhs: SpikeLink, rhs: SpikeLink) -> Bool {
        lhs.label == rhs.label && lhs.next == rhs.next
    }

    public var kotlinValue: __SpikeLink {
        kotlin ?? __SpikeLink(label: label, next: next)
    }
}

public struct SpikeScreen: Equatable {
    public let root: SpikeNode
    public let link: SpikeLink
    public let kotlin: __SpikeScreen?

    public init(root: SpikeNode, link: SpikeLink) {
        self.root = root
        self.link = link
        self.kotlin = nil
    }

    public init(_ kotlin: __SpikeScreen) {
        self.root = SpikeNode(kotlin.root)
        self.link = SpikeLink(kotlin.link)
        self.kotlin = kotlin
    }

    public static func == (lhs: SpikeScreen, rhs: SpikeScreen) -> Bool {
        lhs.root == rhs.root && lhs.link == rhs.link
    }

    public var kotlinValue: __SpikeScreen {
        kotlin ?? __SpikeScreen(root: root.kotlinValue, link: link.kotlinValue)
    }
}

public struct UiError: Equatable {
    public let message: Swift.String
    public let kind: SampleShared.UiErrorKind
    public let retryLabel: Swift.String?
    public let kotlin: __UiError?

    public init(message: Swift.String, kind: SampleShared.UiErrorKind, retryLabel: Swift.String?) {
        self.message = message
        self.kind = kind
        self.retryLabel = retryLabel
        self.kotlin = nil
    }

    public init(_ kotlin: __UiError) {
        self.message = kotlin.message.localized()
        self.kind = kotlin.kind
        self.retryLabel = kotlin.retryLabel?.localized()
        self.kotlin = kotlin
    }

    public static func == (lhs: UiError, rhs: UiError) -> Bool {
        lhs.message == rhs.message && lhs.kind == rhs.kind && lhs.retryLabel == rhs.retryLabel
    }

    public var kotlinValue: __UiError {
        kotlin ?? __UiError(message: RawStringDesc.init(string:)(message), kind: kind, retryLabel: retryLabel.map(RawStringDesc.init(string:)))
    }
}

public struct UiMessage: Equatable {
    public let id: Swift.Int64
    public let text: Swift.String
    public let kind: SampleShared.MessageKind
    public let actionLabel: Swift.String?
    public let durationMillis: Swift.Int64
    public let kotlin: __UiMessage?

    public init(id: Swift.Int64, text: Swift.String, kind: SampleShared.MessageKind, actionLabel: Swift.String?, durationMillis: Swift.Int64) {
        self.id = id
        self.text = text
        self.kind = kind
        self.actionLabel = actionLabel
        self.durationMillis = durationMillis
        self.kotlin = nil
    }

    public init(_ kotlin: __UiMessage) {
        self.id = kotlin.id
        self.text = kotlin.text.localized()
        self.kind = kotlin.kind
        self.actionLabel = kotlin.actionLabel?.localized()
        self.durationMillis = kotlin.durationMillis
        self.kotlin = kotlin
    }

    public static func == (lhs: UiMessage, rhs: UiMessage) -> Bool {
        lhs.id == rhs.id && lhs.text == rhs.text && lhs.kind == rhs.kind && lhs.actionLabel == rhs.actionLabel && lhs.durationMillis == rhs.durationMillis
    }

    public var kotlinValue: __UiMessage {
        kotlin ?? __UiMessage(id: id, text: RawStringDesc.init(string:)(text), kind: kind, actionLabel: actionLabel.map(RawStringDesc.init(string:)), durationMillis: durationMillis)
    }
}

public struct UiDialog: Equatable {
    public let id: Swift.Int64
    public let title: Swift.String?
    public let text: Swift.String
    public let confirm: Swift.String
    public let dismiss: Swift.String?
    public let destructive: Swift.Bool
    public let kotlin: __UiDialog?

    public init(id: Swift.Int64, title: Swift.String?, text: Swift.String, confirm: Swift.String, dismiss: Swift.String?, destructive: Swift.Bool) {
        self.id = id
        self.title = title
        self.text = text
        self.confirm = confirm
        self.dismiss = dismiss
        self.destructive = destructive
        self.kotlin = nil
    }

    public init(_ kotlin: __UiDialog) {
        self.id = kotlin.id
        self.title = kotlin.title?.localized()
        self.text = kotlin.text.localized()
        self.confirm = kotlin.confirm.localized()
        self.dismiss = kotlin.dismiss?.localized()
        self.destructive = kotlin.destructive
        self.kotlin = kotlin
    }

    public static func == (lhs: UiDialog, rhs: UiDialog) -> Bool {
        lhs.id == rhs.id && lhs.title == rhs.title && lhs.text == rhs.text && lhs.confirm == rhs.confirm && lhs.dismiss == rhs.dismiss && lhs.destructive == rhs.destructive
    }

    public var kotlinValue: __UiDialog {
        kotlin ?? __UiDialog(id: id, title: title.map(RawStringDesc.init(string:)), text: RawStringDesc.init(string:)(text), confirm: RawStringDesc.init(string:)(confirm), dismiss: dismiss.map(RawStringDesc.init(string:)), destructive: destructive)
    }
}

public struct ScreenChrome: Equatable {
    public let loading: SampleShared.LoadingKind?
    public let backgroundLoading: Swift.Bool
    public let error: UiError?
    public let messages: [UiMessage]
    public let dialog: UiDialog?
    public let kotlin: __ScreenChrome?

    public init(loading: SampleShared.LoadingKind?, backgroundLoading: Swift.Bool, error: UiError?, messages: [UiMessage], dialog: UiDialog?) {
        self.loading = loading
        self.backgroundLoading = backgroundLoading
        self.error = error
        self.messages = messages
        self.dialog = dialog
        self.kotlin = nil
    }

    public init(_ kotlin: __ScreenChrome) {
        self.loading = kotlin.loading
        self.backgroundLoading = kotlin.backgroundLoading
        self.error = kotlin.error.map(UiError.init)
        self.messages = kotlin.messages.map(UiMessage.init)
        self.dialog = kotlin.dialog.map(UiDialog.init)
        self.kotlin = kotlin
    }

    public static func == (lhs: ScreenChrome, rhs: ScreenChrome) -> Bool {
        lhs.loading == rhs.loading && lhs.backgroundLoading == rhs.backgroundLoading && lhs.error == rhs.error && lhs.messages == rhs.messages && lhs.dialog == rhs.dialog
    }

    public var kotlinValue: __ScreenChrome {
        kotlin ?? __ScreenChrome(loading: loading, backgroundLoading: backgroundLoading, error: error?.kotlinValue, messages: messages.map { $0.kotlinValue }, dialog: dialog?.kotlinValue)
    }
}

public struct Style: Equatable {
    public let padding: SampleShared.Space?
    public let background: SampleShared.ColorToken?
    public let corner: SampleShared.Space?
    public let kotlin: __Style?

    public init(padding: SampleShared.Space?, background: SampleShared.ColorToken?, corner: SampleShared.Space?) {
        self.padding = padding
        self.background = background
        self.corner = corner
        self.kotlin = nil
    }

    public init(_ kotlin: __Style) {
        self.padding = kotlin.padding
        self.background = kotlin.background
        self.corner = kotlin.corner
        self.kotlin = kotlin
    }

    public static func == (lhs: Style, rhs: Style) -> Bool {
        lhs.padding == rhs.padding && lhs.background == rhs.background && lhs.corner == rhs.corner
    }

    public var kotlinValue: __Style {
        kotlin ?? __Style(padding: padding, background: background, corner: corner)
    }
}

public struct A11y: Equatable {
    public let label: Swift.String?
    public let heading: Swift.Bool
    public let hidden: Swift.Bool
    public let kotlin: __A11y?

    public init(label: Swift.String?, heading: Swift.Bool, hidden: Swift.Bool) {
        self.label = label
        self.heading = heading
        self.hidden = hidden
        self.kotlin = nil
    }

    public init(_ kotlin: __A11y) {
        self.label = kotlin.label
        self.heading = kotlin.heading
        self.hidden = kotlin.hidden
        self.kotlin = kotlin
    }

    public static func == (lhs: A11y, rhs: A11y) -> Bool {
        lhs.label == rhs.label && lhs.heading == rhs.heading && lhs.hidden == rhs.hidden
    }

    public var kotlinValue: __A11y {
        kotlin ?? __A11y(label: label, heading: heading, hidden: hidden)
    }
}

public struct ScreenSpec: Equatable {
    public let id: Swift.String
    public let root: Node
    public let schema: Swift.Int32
    public let kotlin: __ScreenSpec?

    public init(id: Swift.String, root: Node, schema: Swift.Int32) {
        self.id = id
        self.root = root
        self.schema = schema
        self.kotlin = nil
    }

    public init(_ kotlin: __ScreenSpec) {
        self.id = kotlin.id
        self.root = Node(kotlin.root)
        self.schema = kotlin.schema
        self.kotlin = kotlin
    }

    public static func == (lhs: ScreenSpec, rhs: ScreenSpec) -> Bool {
        lhs.id == rhs.id && lhs.root == rhs.root && lhs.schema == rhs.schema
    }

    public var kotlinValue: __ScreenSpec {
        kotlin ?? __ScreenSpec(id: id, root: root.kotlinValue, schema: schema)
    }
}

public struct FormState: Equatable {
    public let values: [Swift.String : Swift.String]
    public let errors: [Swift.String : Swift.String]
    public let kotlin: __FormState?

    public init(values: [Swift.String : Swift.String], errors: [Swift.String : Swift.String]) {
        self.values = values
        self.errors = errors
        self.kotlin = nil
    }

    public init(_ kotlin: __FormState) {
        self.values = kotlin.values
        self.errors = kotlin.errors
        self.kotlin = kotlin
    }

    public static func == (lhs: FormState, rhs: FormState) -> Bool {
        lhs.values == rhs.values && lhs.errors == rhs.errors
    }

    public var kotlinValue: __FormState {
        kotlin ?? __FormState(values: values, errors: errors)
    }
}

public struct FixtureState: Equatable {
    public let screen: ScreenSpec
    public let form: FormState
    public let kotlin: __FixtureState?

    public init(screen: ScreenSpec, form: FormState) {
        self.screen = screen
        self.form = form
        self.kotlin = nil
    }

    public init(_ kotlin: __FixtureState) {
        self.screen = ScreenSpec(kotlin.screen)
        self.form = FormState(kotlin.form)
        self.kotlin = kotlin
    }

    public static func == (lhs: FixtureState, rhs: FixtureState) -> Bool {
        lhs.screen == rhs.screen && lhs.form == rhs.form
    }

    public var kotlinValue: __FixtureState {
        kotlin ?? __FixtureState(screen: screen.kotlinValue, form: form.kotlinValue)
    }
}

public struct Order: Equatable {
    public let id: Swift.Int32
    public let title: Swift.String
    public let status: Swift.String
    public let kotlin: __Order?

    public init(id: Swift.Int32, title: Swift.String, status: Swift.String) {
        self.id = id
        self.title = title
        self.status = status
        self.kotlin = nil
    }

    public init(_ kotlin: __Order) {
        self.id = kotlin.id
        self.title = kotlin.title
        self.status = kotlin.status
        self.kotlin = kotlin
    }

    public static func == (lhs: Order, rhs: Order) -> Bool {
        lhs.id == rhs.id && lhs.title == rhs.title && lhs.status == rhs.status
    }

    public var kotlinValue: __Order {
        kotlin ?? __Order(id: id, title: title, status: status)
    }
}

public struct OrdersDetailsState: Equatable {
    public let order: Order?
    public let note: Swift.String
    public let kotlin: __OrdersDetailsState?

    public init(order: Order?, note: Swift.String) {
        self.order = order
        self.note = note
        self.kotlin = nil
    }

    public init(_ kotlin: __OrdersDetailsState) {
        self.order = kotlin.order.map(Order.init)
        self.note = kotlin.note
        self.kotlin = kotlin
    }

    public static func == (lhs: OrdersDetailsState, rhs: OrdersDetailsState) -> Bool {
        lhs.order == rhs.order && lhs.note == rhs.note
    }

    public var kotlinValue: __OrdersDetailsState {
        kotlin ?? __OrdersDetailsState(order: order?.kotlinValue, note: note)
    }
}

public struct OrderFilterState: Equatable {
    public let statuses: [Swift.String]
    public let selected: Swift.String?
    public let kotlin: __OrderFilterState?

    public init(statuses: [Swift.String], selected: Swift.String?) {
        self.statuses = statuses
        self.selected = selected
        self.kotlin = nil
    }

    public init(_ kotlin: __OrderFilterState) {
        self.statuses = kotlin.statuses
        self.selected = kotlin.selected
        self.kotlin = kotlin
    }

    public static func == (lhs: OrderFilterState, rhs: OrderFilterState) -> Bool {
        lhs.statuses == rhs.statuses && lhs.selected == rhs.selected
    }

    public var kotlinValue: __OrderFilterState {
        kotlin ?? __OrderFilterState(statuses: statuses, selected: selected)
    }
}

public struct OrdersState: Equatable {
    public let orders: [Order]
    public let view: OrdersView
    public let kotlin: __OrdersState?

    public init(orders: [Order], view: OrdersView) {
        self.orders = orders
        self.view = view
        self.kotlin = nil
    }

    public init(_ kotlin: __OrdersState) {
        self.orders = kotlin.orders.map(Order.init)
        self.view = OrdersView(kotlin.view)
        self.kotlin = kotlin
    }

    public static func == (lhs: OrdersState, rhs: OrdersState) -> Bool {
        lhs.orders == rhs.orders && lhs.view == rhs.view
    }

    public var kotlinValue: __OrdersState {
        kotlin ?? __OrdersState(orders: orders.map { $0.kotlinValue }, view: view.kotlinValue)
    }
}

public enum ProfileAction: Equatable {
    case navigateBack
    case showToast(text: Swift.String)

    public init(_ kotlin: __ProfileAction) {
        if kotlin is ProfileActionNavigateBack { self = .navigateBack; return }
        if let c = kotlin as? ProfileActionShowToast { self = .showToast(text: c.text); return }
        fatalError("Unknown subclass of ProfileAction")
    }

    public var kotlinValue: __ProfileAction {
        switch self {
        case .navigateBack: return ProfileActionNavigateBack.shared
        case .showToast(let text): return ProfileActionShowToast(text: text)
        }
    }
}

public enum ProfileScreenState: Equatable {
    case error(message: Swift.String, code: Int32?)
    case idle
    case loading

    public init(_ kotlin: __ProfileScreenState) {
        if let c = kotlin as? ProfileScreenStateError { self = .error(message: c.message, code: c.code?.int32Value); return }
        if kotlin is ProfileScreenStateIdle { self = .idle; return }
        if kotlin is ProfileScreenStateLoading { self = .loading; return }
        fatalError("Unknown subclass of ProfileScreenState")
    }

    public var kotlinValue: __ProfileScreenState {
        switch self {
        case .error(let message, let code): return ProfileScreenStateError(message: message, code: code.map { KotlinInt(int: $0) })
        case .idle: return ProfileScreenStateIdle.shared
        case .loading: return ProfileScreenStateLoading.shared
        }
    }
}

public indirect enum SpikeNode: Equatable {
    case card(id: Swift.String, content: SpikeNode)
    case column(id: Swift.String, children: [SpikeNode])
    case sections(id: Swift.String, items: [SpikeSection])
    case text(id: Swift.String, text: Swift.String)

    public init(_ kotlin: __SpikeNode) {
        if let c = kotlin as? SpikeNodeCard { self = .card(id: c.id, content: SpikeNode(c.content)); return }
        if let c = kotlin as? SpikeNodeColumn { self = .column(id: c.id, children: c.children.map(SpikeNode.init)); return }
        if let c = kotlin as? SpikeNodeSections { self = .sections(id: c.id, items: c.items.map(SpikeSection.init)); return }
        if let c = kotlin as? SpikeNodeText { self = .text(id: c.id, text: c.text); return }
        fatalError("Unknown subclass of SpikeNode")
    }

    public var id: Swift.String {
        switch self {
        case .card(let id, _): return id
        case .column(let id, _): return id
        case .sections(let id, _): return id
        case .text(let id, _): return id
        }
    }

    public var kotlinValue: __SpikeNode {
        switch self {
        case .card(let id, let content): return SpikeNodeCard(id: id, content: content.kotlinValue)
        case .column(let id, let children): return SpikeNodeColumn(id: id, children: children.map { $0.kotlinValue })
        case .sections(let id, let items): return SpikeNodeSections(id: id, items: items.map { $0.kotlinValue })
        case .text(let id, let text): return SpikeNodeText(id: id, text: text)
        }
    }
}

public enum OrdersAction: Equatable {
    case scrollToTop

    public init(_ kotlin: __OrdersAction) {
        if kotlin is OrdersActionScrollToTop { self = .scrollToTop; return }
        fatalError("Unknown subclass of OrdersAction")
    }

    public var kotlinValue: __OrdersAction {
        switch self {
        case .scrollToTop: return OrdersActionScrollToTop.shared
        }
    }
}

public enum NodeEvent: Equatable {
    case click(id: Swift.String)
    case custom(key: Swift.String, payload: [Swift.String : Swift.String])
    case navigate(destination: Swift.String)
    case submit
    case unknown
    case valueChanged(field: Swift.String, value: Swift.String)

    public init(_ kotlin: __NodeEvent) {
        if let c = kotlin as? NodeEventClick { self = .click(id: c.id); return }
        if let c = kotlin as? NodeEventCustom { self = .custom(key: c.key, payload: c.payload); return }
        if let c = kotlin as? NodeEventNavigate { self = .navigate(destination: c.destination); return }
        if kotlin is NodeEventSubmit { self = .submit; return }
        if kotlin is NodeEventUnknown { self = .unknown; return }
        if let c = kotlin as? NodeEventValueChanged { self = .valueChanged(field: c.field, value: c.value); return }
        fatalError("Unknown subclass of NodeEvent")
    }

    public var kotlinValue: __NodeEvent {
        switch self {
        case .click(let id): return NodeEventClick(id: id)
        case .custom(let key, let payload): return NodeEventCustom(key: key, payload: payload)
        case .navigate(let destination): return NodeEventNavigate(destination: destination)
        case .submit: return NodeEventSubmit.shared
        case .unknown: return NodeEventUnknown.shared
        case .valueChanged(let field, let value): return NodeEventValueChanged(field: field, value: value)
        }
    }
}

public enum InputRule: Equatable {
    case minLength(length: Swift.Int32, message: Swift.String)
    case pattern(regex: Swift.String, message: Swift.String)
    case required(message: Swift.String)
    case unknown(message: Swift.String)

    public init(_ kotlin: __InputRule) {
        if let c = kotlin as? InputRuleMinLength { self = .minLength(length: c.length, message: c.message); return }
        if let c = kotlin as? InputRulePattern { self = .pattern(regex: c.regex, message: c.message); return }
        if let c = kotlin as? InputRuleRequired { self = .required(message: c.message); return }
        if let c = kotlin as? InputRuleUnknown { self = .unknown(message: c.message); return }
        fatalError("Unknown subclass of InputRule")
    }

    public var message: Swift.String {
        switch self {
        case .minLength(_, let message): return message
        case .pattern(_, let message): return message
        case .required(let message): return message
        case .unknown(let message): return message
        }
    }

    public var kotlinValue: __InputRule {
        switch self {
        case .minLength(let length, let message): return InputRuleMinLength(length: length, message: message)
        case .pattern(let regex, let message): return InputRulePattern(regex: regex, message: message)
        case .required(let message): return InputRuleRequired(message: message)
        case .unknown(let message): return InputRuleUnknown(message: message)
        }
    }
}

public indirect enum Node: Equatable {
    case box(id: Swift.String, children: [Node], event: NodeEvent?, style: Style?, a11y: A11y?)
    case button(id: Swift.String, label: Swift.String, event: NodeEvent, variant: SampleShared.ButtonVariant, style: Style?, a11y: A11y?)
    case column(id: Swift.String, children: [Node], spacing: SampleShared.Space?, align: SampleShared.HAlign, scroll: Swift.Bool, event: NodeEvent?, style: Style?, a11y: A11y?)
    case custom(id: Swift.String, key: Swift.String, props: [Swift.String : Swift.String], style: Style?, a11y: A11y?)
    case image(id: Swift.String, name: Swift.String, size: Swift.Int32, style: Style?, a11y: A11y?)
    case input(id: Swift.String, field: Swift.String, label: Swift.String, kind: SampleShared.InputKind, rules: [InputRule], style: Style?, a11y: A11y?)
    case lazyList(id: Swift.String, items: [Node], axis: SampleShared.ListAxis, spacing: SampleShared.Space?, size: Int32?, style: Style?, a11y: A11y?)
    case row(id: Swift.String, children: [Node], spacing: SampleShared.Space?, align: SampleShared.VAlign, scroll: Swift.Bool, event: NodeEvent?, style: Style?, a11y: A11y?)
    case spacer(id: Swift.String, size: SampleShared.Space?, flexible: Swift.Bool, style: Style?, a11y: A11y?)
    case text(id: Swift.String, text: Swift.String, textStyle: SampleShared.TextStyleToken, color: SampleShared.ColorToken?, style: Style?, a11y: A11y?)
    case toggle(id: Swift.String, field: Swift.String, label: Swift.String, style: Style?, a11y: A11y?)
    case unknown(id: Swift.String, fallback: Node?, style: Style?, a11y: A11y?, error: Swift.String?)

    public init(_ kotlin: __Node) {
        if let c = kotlin as? NodeBox { self = .box(id: c.id, children: c.children.map(Node.init), event: c.event.map(NodeEvent.init), style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeButton { self = .button(id: c.id, label: c.label, event: NodeEvent(c.event), variant: c.variant, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeColumn { self = .column(id: c.id, children: c.children.map(Node.init), spacing: c.spacing, align: c.align, scroll: c.scroll, event: c.event.map(NodeEvent.init), style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeCustom { self = .custom(id: c.id, key: c.key, props: c.props, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeImage { self = .image(id: c.id, name: c.name, size: c.size, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeInput { self = .input(id: c.id, field: c.field, label: c.label, kind: c.kind, rules: c.rules.map(InputRule.init), style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeLazyList { self = .lazyList(id: c.id, items: c.items.map(Node.init), axis: c.axis, spacing: c.spacing, size: c.size?.int32Value, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeRow { self = .row(id: c.id, children: c.children.map(Node.init), spacing: c.spacing, align: c.align, scroll: c.scroll, event: c.event.map(NodeEvent.init), style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeSpacer { self = .spacer(id: c.id, size: c.size, flexible: c.flexible, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeText { self = .text(id: c.id, text: c.text, textStyle: c.textStyle, color: c.color, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeToggle { self = .toggle(id: c.id, field: c.field, label: c.label, style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init)); return }
        if let c = kotlin as? NodeUnknown { self = .unknown(id: c.id, fallback: c.fallback.map(Node.init), style: c.style.map(Style.init), a11y: c.a11y.map(A11y.init), error: c.error); return }
        fatalError("Unknown subclass of Node")
    }

    public var id: Swift.String {
        switch self {
        case .box(let id, _, _, _, _): return id
        case .button(let id, _, _, _, _, _): return id
        case .column(let id, _, _, _, _, _, _, _): return id
        case .custom(let id, _, _, _, _): return id
        case .image(let id, _, _, _, _): return id
        case .input(let id, _, _, _, _, _, _): return id
        case .lazyList(let id, _, _, _, _, _, _): return id
        case .row(let id, _, _, _, _, _, _, _): return id
        case .spacer(let id, _, _, _, _): return id
        case .text(let id, _, _, _, _, _): return id
        case .toggle(let id, _, _, _, _): return id
        case .unknown(let id, _, _, _, _): return id
        }
    }

    public var style: Style? {
        switch self {
        case .box(_, _, _, let style, _): return style
        case .button(_, _, _, _, let style, _): return style
        case .column(_, _, _, _, _, _, let style, _): return style
        case .custom(_, _, _, let style, _): return style
        case .image(_, _, _, let style, _): return style
        case .input(_, _, _, _, _, let style, _): return style
        case .lazyList(_, _, _, _, _, let style, _): return style
        case .row(_, _, _, _, _, _, let style, _): return style
        case .spacer(_, _, _, let style, _): return style
        case .text(_, _, _, _, let style, _): return style
        case .toggle(_, _, _, let style, _): return style
        case .unknown(_, _, let style, _, _): return style
        }
    }

    public var a11y: A11y? {
        switch self {
        case .box(_, _, _, _, let a11y): return a11y
        case .button(_, _, _, _, _, let a11y): return a11y
        case .column(_, _, _, _, _, _, _, let a11y): return a11y
        case .custom(_, _, _, _, let a11y): return a11y
        case .image(_, _, _, _, let a11y): return a11y
        case .input(_, _, _, _, _, _, let a11y): return a11y
        case .lazyList(_, _, _, _, _, _, let a11y): return a11y
        case .row(_, _, _, _, _, _, _, let a11y): return a11y
        case .spacer(_, _, _, _, let a11y): return a11y
        case .text(_, _, _, _, _, let a11y): return a11y
        case .toggle(_, _, _, _, let a11y): return a11y
        case .unknown(_, _, _, let a11y, _): return a11y
        }
    }

    public var kotlinValue: __Node {
        switch self {
        case .box(let id, let children, let event, let style, let a11y): return NodeBox(id: id, children: children.map { $0.kotlinValue }, event: event?.kotlinValue, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .button(let id, let label, let event, let variant, let style, let a11y): return NodeButton(id: id, label: label, event: event.kotlinValue, variant: variant, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .column(let id, let children, let spacing, let align, let scroll, let event, let style, let a11y): return NodeColumn(id: id, children: children.map { $0.kotlinValue }, spacing: spacing, align: align, scroll: scroll, event: event?.kotlinValue, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .custom(let id, let key, let props, let style, let a11y): return NodeCustom(id: id, key: key, props: props, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .image(let id, let name, let size, let style, let a11y): return NodeImage(id: id, name: name, size: size, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .input(let id, let field, let label, let kind, let rules, let style, let a11y): return NodeInput(id: id, field: field, label: label, kind: kind, rules: rules.map { $0.kotlinValue }, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .lazyList(let id, let items, let axis, let spacing, let size, let style, let a11y): return NodeLazyList(id: id, items: items.map { $0.kotlinValue }, axis: axis, spacing: spacing, size: size.map { KotlinInt(int: $0) }, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .row(let id, let children, let spacing, let align, let scroll, let event, let style, let a11y): return NodeRow(id: id, children: children.map { $0.kotlinValue }, spacing: spacing, align: align, scroll: scroll, event: event?.kotlinValue, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .spacer(let id, let size, let flexible, let style, let a11y): return NodeSpacer(id: id, size: size, flexible: flexible, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .text(let id, let text, let textStyle, let color, let style, let a11y): return NodeText(id: id, text: text, textStyle: textStyle, color: color, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .toggle(let id, let field, let label, let style, let a11y): return NodeToggle(id: id, field: field, label: label, style: style?.kotlinValue, a11y: a11y?.kotlinValue)
        case .unknown(let id, let fallback, let style, let a11y, let error): return NodeUnknown(id: id, fallback: fallback?.kotlinValue, style: style?.kotlinValue, a11y: a11y?.kotlinValue, error: error)
        }
    }
}

public enum OrdersView: Equatable {
    case all
    case filtered(status: Swift.String)

    public init(_ kotlin: __OrdersView) {
        if kotlin is OrdersViewAll { self = .all; return }
        if let c = kotlin as? OrdersViewFiltered { self = .filtered(status: c.status); return }
        fatalError("Unknown subclass of OrdersView")
    }

    public var kotlinValue: __OrdersView {
        switch self {
        case .all: return OrdersViewAll.shared
        case .filtered(let status): return OrdersViewFiltered(status: status)
        }
    }
}

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

public enum OrdersRoute: Hashable {
    case details(id: Swift.Int32)
    case filter(preset: Swift.String?)
    case fixture(name: Swift.String)
    case list
    case profile

    public init(_ kotlin: __OrdersRoute) {
        if let c = kotlin as? OrdersRouteDetails { self = .details(id: c.id); return }
        if let c = kotlin as? OrdersRouteFilter { self = .filter(preset: c.preset); return }
        if let c = kotlin as? OrdersRouteFixture { self = .fixture(name: c.name); return }
        if kotlin is OrdersRouteList { self = .list; return }
        if kotlin is OrdersRouteProfile { self = .profile; return }
        fatalError("Unknown subclass of OrdersRoute")
    }

    public init?(route: Route) {
        guard let kotlin = route as? __OrdersRoute else { return nil }
        self.init(kotlin)
    }

    public var kotlinValue: __OrdersRoute {
        switch self {
        case .details(let id): return OrdersRouteDetails(id: id)
        case .filter(let preset): return OrdersRouteFilter(preset: preset)
        case .fixture(let name): return OrdersRouteFixture(name: name)
        case .list: return OrdersRouteList.shared
        case .profile: return OrdersRouteProfile.shared
        }
    }
}

private let LockstepKey_StackNavigator_stack: StaticString = "LockstepKey_StackNavigator_stack"

@available(iOS 17.0, *)
extension StackNavigator {
    public var stack: UiState<NavStack> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_StackNavigator_stack.utf8Start) as? UiState<NavStack> {
            return cached
        }
        let created = UiState(__stack, { kotlin in NavStack(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_StackNavigator_stack.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_FixtureNavigation_state: StaticString = "LockstepKey_FixtureNavigation_state"

@available(iOS 17.0, *)
extension FixtureNavigation {
    public var state: UiState<MappingFixtureState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_FixtureNavigation_state.utf8Start) as? UiState<MappingFixtureState> {
            return cached
        }
        let created = UiState(__state, { kotlin in MappingFixtureState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_FixtureNavigation_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension ListViewModel {
    public var actions: UiEvents<ProfileAction> {
        UiEvents(__actions, { kotlin in ProfileAction(kotlin!) })
    }
}

private let LockstepKey_ListViewModel_state: StaticString = "LockstepKey_ListViewModel_state"

@available(iOS 17.0, *)
extension ListViewModel {
    public var state: UiState<ListState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ListViewModel_state.utf8Start) as? UiState<ListState> {
            return cached
        }
        let created = UiState(__state, { kotlin in ListState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_ListViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension MappingFixtureViewModel {
    public var actions: UiEvents<ProfileAction> {
        UiEvents(__actions, { kotlin in ProfileAction(kotlin!) })
    }
}

private let LockstepKey_MappingFixtureViewModel_state: StaticString = "LockstepKey_MappingFixtureViewModel_state"

@available(iOS 17.0, *)
extension MappingFixtureViewModel {
    public var state: UiState<MappingFixtureState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_MappingFixtureViewModel_state.utf8Start) as? UiState<MappingFixtureState> {
            return cached
        }
        let created = UiState(__state, { kotlin in MappingFixtureState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_MappingFixtureViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension ProfileViewModel {
    public var actions: UiEvents<ProfileAction> {
        UiEvents(__actions, { kotlin in ProfileAction(kotlin!) })
    }
}

@available(iOS 17.0, *)
extension ProfileViewModel {
    public var finiteEvents: UiEvents<ProfileAction> {
        UiEvents(__finiteEvents, { kotlin in ProfileAction(kotlin!) })
    }
}

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
extension SettingsViewModel {
    public var actions: UiEvents<ProfileAction> {
        UiEvents(__actions, { kotlin in ProfileAction(kotlin!) })
    }
}

private let LockstepKey_SettingsViewModel_state: StaticString = "LockstepKey_SettingsViewModel_state"

@available(iOS 17.0, *)
extension SettingsViewModel {
    public var state: UiState<SettingsState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_SettingsViewModel_state.utf8Start) as? UiState<SettingsState> {
            return cached
        }
        let created = UiState(__state, { kotlin in SettingsState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_SettingsViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension SpikeScreenViewModel {
    public var actions: UiEvents<ProfileAction> {
        UiEvents(__actions, { kotlin in ProfileAction(kotlin!) })
    }
}

private let LockstepKey_SpikeScreenViewModel_state: StaticString = "LockstepKey_SpikeScreenViewModel_state"

@available(iOS 17.0, *)
extension SpikeScreenViewModel {
    public var state: UiState<SpikeScreen> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_SpikeScreenViewModel_state.utf8Start) as? UiState<SpikeScreen> {
            return cached
        }
        let created = UiState(__state, { kotlin in SpikeScreen(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_SpikeScreenViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_addresses: StaticString = "LockstepKey_ValueFlows_addresses"

@available(iOS 17.0, *)
extension ValueFlows {
    public var addresses: UiState<[Address]> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_addresses.utf8Start) as? UiState<[Address]> {
            return cached
        }
        let created = UiState(__addresses, { kotlin in (kotlin! as! [__Address]).map { Address($0) } })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_addresses.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension ValueFlows {
    public var clicks: UiEvents<Int32> {
        UiEvents(__clicks, { kotlin in kotlin!.int32Value })
    }
}

private let LockstepKey_ValueFlows_count: StaticString = "LockstepKey_ValueFlows_count"

@available(iOS 17.0, *)
extension ValueFlows {
    public var count: UiState<Int32> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_count.utf8Start) as? UiState<Int32> {
            return cached
        }
        let created = UiState(__count, { kotlin in kotlin!.int32Value })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_count.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_enabled: StaticString = "LockstepKey_ValueFlows_enabled"

@available(iOS 17.0, *)
extension ValueFlows {
    public var enabled: UiState<Bool> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_enabled.utf8Start) as? UiState<Bool> {
            return cached
        }
        let created = UiState(__enabled, { kotlin in kotlin!.boolValue })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_enabled.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_holder: StaticString = "LockstepKey_ValueFlows_holder"

@available(iOS 17.0, *)
extension ValueFlows {
    public var holder: UiState<SampleShared.FixtureStateHolder> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_holder.utf8Start) as? UiState<SampleShared.FixtureStateHolder> {
            return cached
        }
        let created = UiState(__holder, { kotlin in (kotlin! as AnyObject) as! SampleShared.FixtureStateHolder })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_holder.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_label: StaticString = "LockstepKey_ValueFlows_label"

@available(iOS 17.0, *)
extension ValueFlows {
    public var label: UiState<Swift.String> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_label.utf8Start) as? UiState<Swift.String> {
            return cached
        }
        let created = UiState(__label, { kotlin in kotlin!.localized() })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_label.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_maybeAddresses: StaticString = "LockstepKey_ValueFlows_maybeAddresses"

@available(iOS 17.0, *)
extension ValueFlows {
    public var maybeAddresses: UiState<[Address]?> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_maybeAddresses.utf8Start) as? UiState<[Address]?> {
            return cached
        }
        let created = UiState(__maybeAddresses, { kotlin in kotlin.map { kotlin in (kotlin as! [__Address]).map { Address($0) } } })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_maybeAddresses.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_maybeTitle: StaticString = "LockstepKey_ValueFlows_maybeTitle"

@available(iOS 17.0, *)
extension ValueFlows {
    public var maybeTitle: UiState<Swift.String?> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_maybeTitle.utf8Start) as? UiState<Swift.String?> {
            return cached
        }
        let created = UiState(__maybeTitle, { kotlin in kotlin.map { kotlin in (kotlin as AnyObject) as! Swift.String } })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_maybeTitle.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_mode: StaticString = "LockstepKey_ValueFlows_mode"

@available(iOS 17.0, *)
extension ValueFlows {
    public var mode: UiState<SampleShared.FixtureMode> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_mode.utf8Start) as? UiState<SampleShared.FixtureMode> {
            return cached
        }
        let created = UiState(__mode, { kotlin in (kotlin! as AnyObject) as! SampleShared.FixtureMode })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_mode.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_repository: StaticString = "LockstepKey_ValueFlows_repository"

@available(iOS 17.0, *)
extension ValueFlows {
    public var repository: UiState<SampleShared.OrdersRepository> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_repository.utf8Start) as? UiState<SampleShared.OrdersRepository> {
            return cached
        }
        let created = UiState(__repository, { kotlin in (kotlin! as AnyObject) as! SampleShared.OrdersRepository })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_repository.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_scores: StaticString = "LockstepKey_ValueFlows_scores"

@available(iOS 17.0, *)
extension ValueFlows {
    public var scores: UiState<[Swift.String : SampleShared.KotlinInt]> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_scores.utf8Start) as? UiState<[Swift.String : SampleShared.KotlinInt]> {
            return cached
        }
        let created = UiState(__scores, { kotlin in (kotlin! as AnyObject) as! [Swift.String : SampleShared.KotlinInt] })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_scores.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_selected: StaticString = "LockstepKey_ValueFlows_selected"

@available(iOS 17.0, *)
extension ValueFlows {
    public var selected: UiState<Address?> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_selected.utf8Start) as? UiState<Address?> {
            return cached
        }
        let created = UiState(__selected, { kotlin in kotlin.map { kotlin in Address(kotlin) } })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_selected.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_tags: StaticString = "LockstepKey_ValueFlows_tags"

@available(iOS 17.0, *)
extension ValueFlows {
    public var tags: UiState<[Swift.String]> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_tags.utf8Start) as? UiState<[Swift.String]> {
            return cached
        }
        let created = UiState(__tags, { kotlin in (kotlin! as AnyObject) as! [Swift.String] })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_tags.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension ValueFlows {
    public var ticks: UiEvents<SampleShared.KotlinUnit> {
        UiEvents(__ticks, { kotlin in (kotlin! as AnyObject) as! SampleShared.KotlinUnit })
    }
}

private let LockstepKey_ValueFlows_title: StaticString = "LockstepKey_ValueFlows_title"

@available(iOS 17.0, *)
extension ValueFlows {
    public var title: UiState<Swift.String> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_title.utf8Start) as? UiState<Swift.String> {
            return cached
        }
        let created = UiState(__title, { kotlin in (kotlin! as AnyObject) as! Swift.String })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_title.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_ValueFlows_total: StaticString = "LockstepKey_ValueFlows_total"

@available(iOS 17.0, *)
extension ValueFlows {
    public var total: UiState<Int64?> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_ValueFlows_total.utf8Start) as? UiState<Int64?> {
            return cached
        }
        let created = UiState(__total, { kotlin in kotlin.map { kotlin in kotlin.int64Value } })
        objc_setAssociatedObject(self, LockstepKey_ValueFlows_total.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension FixtureViewModel {
    public var actions: UiEvents<OrdersAction> {
        UiEvents(__actions, { kotlin in OrdersAction(kotlin!) })
    }
}

private let LockstepKey_FixtureViewModel_chrome: StaticString = "LockstepKey_FixtureViewModel_chrome"

@available(iOS 17.0, *)
extension FixtureViewModel {
    public var chrome: UiState<ScreenChrome> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_FixtureViewModel_chrome.utf8Start) as? UiState<ScreenChrome> {
            return cached
        }
        let created = UiState(__chrome, { kotlin in ScreenChrome(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_FixtureViewModel_chrome.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_FixtureViewModel_state: StaticString = "LockstepKey_FixtureViewModel_state"

@available(iOS 17.0, *)
extension FixtureViewModel {
    public var state: UiState<FixtureState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_FixtureViewModel_state.utf8Start) as? UiState<FixtureState> {
            return cached
        }
        let created = UiState(__state, { kotlin in FixtureState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_FixtureViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension OrderDetailsViewModel {
    public var actions: UiEvents<OrdersAction> {
        UiEvents(__actions, { kotlin in OrdersAction(kotlin!) })
    }
}

private let LockstepKey_OrderDetailsViewModel_chrome: StaticString = "LockstepKey_OrderDetailsViewModel_chrome"

@available(iOS 17.0, *)
extension OrderDetailsViewModel {
    public var chrome: UiState<ScreenChrome> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_OrderDetailsViewModel_chrome.utf8Start) as? UiState<ScreenChrome> {
            return cached
        }
        let created = UiState(__chrome, { kotlin in ScreenChrome(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_OrderDetailsViewModel_chrome.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_OrderDetailsViewModel_state: StaticString = "LockstepKey_OrderDetailsViewModel_state"

@available(iOS 17.0, *)
extension OrderDetailsViewModel {
    public var state: UiState<OrdersDetailsState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_OrderDetailsViewModel_state.utf8Start) as? UiState<OrdersDetailsState> {
            return cached
        }
        let created = UiState(__state, { kotlin in OrdersDetailsState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_OrderDetailsViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension OrderFilterViewModel {
    public var actions: UiEvents<OrdersAction> {
        UiEvents(__actions, { kotlin in OrdersAction(kotlin!) })
    }
}

private let LockstepKey_OrderFilterViewModel_chrome: StaticString = "LockstepKey_OrderFilterViewModel_chrome"

@available(iOS 17.0, *)
extension OrderFilterViewModel {
    public var chrome: UiState<ScreenChrome> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_OrderFilterViewModel_chrome.utf8Start) as? UiState<ScreenChrome> {
            return cached
        }
        let created = UiState(__chrome, { kotlin in ScreenChrome(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_OrderFilterViewModel_chrome.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_OrderFilterViewModel_state: StaticString = "LockstepKey_OrderFilterViewModel_state"

@available(iOS 17.0, *)
extension OrderFilterViewModel {
    public var state: UiState<OrderFilterState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_OrderFilterViewModel_state.utf8Start) as? UiState<OrderFilterState> {
            return cached
        }
        let created = UiState(__state, { kotlin in OrderFilterState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_OrderFilterViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

@available(iOS 17.0, *)
extension OrdersViewModel {
    public var actions: UiEvents<OrdersAction> {
        UiEvents(__actions, { kotlin in OrdersAction(kotlin!) })
    }
}

private let LockstepKey_OrdersViewModel_chrome: StaticString = "LockstepKey_OrdersViewModel_chrome"

@available(iOS 17.0, *)
extension OrdersViewModel {
    public var chrome: UiState<ScreenChrome> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_OrdersViewModel_chrome.utf8Start) as? UiState<ScreenChrome> {
            return cached
        }
        let created = UiState(__chrome, { kotlin in ScreenChrome(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_OrdersViewModel_chrome.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}

private let LockstepKey_OrdersViewModel_state: StaticString = "LockstepKey_OrdersViewModel_state"

@available(iOS 17.0, *)
extension OrdersViewModel {
    public var state: UiState<OrdersState> {
        if let cached = objc_getAssociatedObject(self, LockstepKey_OrdersViewModel_state.utf8Start) as? UiState<OrdersState> {
            return cached
        }
        let created = UiState(__state, { kotlin in OrdersState(kotlin!) })
        objc_setAssociatedObject(self, LockstepKey_OrdersViewModel_state.utf8Start, created, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return created
    }
}
