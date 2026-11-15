package io.github.vad4nus.lockstep.skie.render

/** Types the plugin generates that the screens runtime refers to; it is emitted only when all of them keep these names. */
internal val screensSwiftTypes: List<String> = listOf("Node", "NodeEvent", "FormState", "Style", "A11y")

/** Kotlin enums the screens runtime refers to; it is emitted only when all of them keep these names in Swift. */
internal val screensSwiftEnums: List<String> =
    listOf("Space", "ColorToken", "TextStyleToken", "ButtonVariant", "InputKind", "HAlign", "VAlign", "ListAxis")

/** Swift types the screens runtime declares; no exported Kotlin class may take these names. */
internal val screensRuntimeSwiftTypes: List<String> = listOf("NodeTheme", "NodeView")

internal fun screensRuntimeSwift(): String = """
    import Foundation
    import SwiftUI
    import UIKit

    /// What the node tokens mean in the app: sizes, colors and fonts, and the images and native pieces by name.
    @available(iOS 17.0, *)
    public struct NodeTheme {
        public var space: (Space) -> CGFloat
        public var color: (ColorToken) -> Color
        public var font: (TextStyleToken) -> Font
        public var images: [String: Image]
        public var custom: [String: ([String: String]) -> AnyView]

        public init(
            space: @escaping (Space) -> CGFloat,
            color: @escaping (ColorToken) -> Color,
            font: @escaping (TextStyleToken) -> Font,
            images: [String: Image] = [:],
            custom: [String: ([String: String]) -> AnyView] = [:]
        ) {
            self.space = space
            self.color = color
            self.font = font
            self.images = images
            self.custom = custom
        }

        public static let standard = NodeTheme(
            space: { token in
                switch token {
                case .xs: return 4
                case .s: return 8
                case .m: return 16
                case .l: return 24
                case .xl: return 32
                }
            },
            color: { token in
                switch token {
                case .background: return Color(uiColor: .systemBackground)
                case .surface: return Color(uiColor: .secondarySystemBackground)
                case .primary: return .accentColor
                case .secondary: return .secondary
                case .error: return .red
                }
            },
            font: { token in
                switch token {
                case .title: return .title
                case .headline: return .headline
                case .body: return .body
                case .caption: return .caption
                }
            }
        )
    }

    @available(iOS 17.0, *)
    private struct NodeThemeKey: EnvironmentKey {
        static let defaultValue = NodeTheme.standard
    }

    @available(iOS 17.0, *)
    extension EnvironmentValues {
        public var nodeTheme: NodeTheme {
            get { self[NodeThemeKey.self] }
            set { self[NodeThemeKey.self] = newValue }
        }
    }

    extension Node: Identifiable {}

    /// Draws a node of a screen described in KMP. Draw a checked tree only: one from `ScreenSpec.decode`,
    /// the `screen` builder or `checked`. An image or a native piece the theme does not have keeps its place
    /// and is logged. A node with an event this version does not know does nothing when tapped.
    @available(iOS 17.0, *)
    public struct NodeView: View {
        private let node: Node
        private let form: FormState
        private let onEvent: (NodeEvent) -> Void
        @Environment(\.nodeTheme) private var theme

        public init(_ node: Node, form: FormState, onEvent: @escaping (NodeEvent) -> Void) {
            self.node = node
            self.form = form
            self.onEvent = onEvent
        }

        public var body: some View {
            content.nodeStyle(node.style, node.a11y, theme, onTap: tap)
        }

        private var tap: (() -> Void)? {
            switch node {
            case .column(_, _, _, _, _, let event, _, _), .row(_, _, _, _, _, let event, _, _), .box(_, _, let event, _, _):
                guard let event, event.known else { return nil }
                return { onEvent(event) }
            default:
                return nil
            }
        }

        @ViewBuilder
        private var content: some View {
            switch node {
            case .column(_, let children, let spacing, let align, let scroll, _, _, _):
                let stack = VStack(alignment: align.alignment, spacing: gap(spacing)) { nodes(children) }
                if scroll {
                    ScrollView(.vertical) { stack.frame(maxWidth: .infinity, alignment: .leading) }
                } else {
                    stack
                }
            case .row(_, let children, let spacing, let align, let scroll, _, _, _):
                let stack = HStack(alignment: align.alignment, spacing: gap(spacing)) { nodes(children) }
                if scroll {
                    ScrollView(.horizontal) { stack }
                } else {
                    stack
                }
            case .box(_, let children, _, _, _):
                ZStack(alignment: .topLeading) { nodes(children) }
            case .lazyList(_, let items, let axis, let spacing, let size, _, _):
                switch axis {
                case .vertical:
                    ScrollView(.vertical) {
                        LazyVStack(alignment: .leading, spacing: gap(spacing)) { nodes(items) }
                    }
                    .frame(height: size.map { CGFloat($0) })
                case .horizontal:
                    ScrollView(.horizontal) {
                        LazyHStack(spacing: gap(spacing)) { nodes(items) }
                    }
                    .frame(width: size.map { CGFloat($0) })
                }
            case .spacer(_, let size, let flexible, _, _):
                if flexible {
                    Spacer(minLength: 0)
                } else {
                    Color.clear.frame(width: gap(size), height: gap(size))
                }
            case .text(_, let text, let textStyle, let color, _, _):
                Text(text)
                    .font(theme.font(textStyle))
                    .foregroundStyle(color.map(theme.color) ?? Color.primary)
            case .image(_, let name, let size, _, _):
                if let image = theme.images[name] {
                    image.resizable().scaledToFit().frame(width: CGFloat(size), height: CGFloat(size))
                } else {
                    Color.clear.frame(width: CGFloat(size), height: CGFloat(size)).onAppear { missing("image", name) }
                }
            case .button(_, let label, let event, let variant, _, _):
                let button = Button(label) { onEvent(event) }.disabled(!event.known)
                switch variant {
                case .primary: button.buttonStyle(.borderedProminent)
                case .secondary: button.buttonStyle(.bordered)
                case .text: button.buttonStyle(.borderless)
                }
            case .input(_, let field, let label, let kind, _, _, _):
                let text = Binding(
                    get: { form.values[field] ?? "" },
                    set: { value in onEvent(.valueChanged(field: field, value: value)) }
                )
                VStack(alignment: .leading, spacing: 4) {
                    Group {
                        if kind == .password {
                            SecureField(label, text: text)
                        } else {
                            TextField(label, text: text)
                        }
                    }
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(kind.keyboard)
                    .textContentType(kind.contentType)
                    .textInputAutocapitalization(kind == .text ? .sentences : .never)
                    if let error = form.errors[field] {
                        Text(error).font(theme.font(.caption)).foregroundStyle(theme.color(.error))
                    }
                }
            case .toggle(_, let field, let label, _, _):
                Toggle(label, isOn: Binding(
                    get: { form.values[field] == "true" },
                    set: { isOn in onEvent(.valueChanged(field: field, value: isOn ? "true" : "false")) }
                ))
            case .custom(_, let key, let props, _, _):
                if let view = theme.custom[key] {
                    view(props)
                } else {
                    Color.clear.frame(width: 0, height: 0).onAppear { missing("custom piece", key) }
                }
            case .unknown(_, let fallback, _, _, _):
                if let fallback {
                    NodeView(fallback, form: form, onEvent: onEvent)
                }
            }
        }

        private func nodes(_ children: [Node]) -> some View {
            ForEach(children) { child in NodeView(child, form: form, onEvent: onEvent) }
        }

        private func gap(_ token: Space?) -> CGFloat {
            token.map(theme.space) ?? 0
        }

        private func missing(_ kind: String, _ name: String) {
            NSLog("lockstep: NodeTheme has no %@ %@", kind, name)
        }
    }

    @available(iOS 17.0, *)
    extension View {
        @ViewBuilder
        fileprivate func nodeTap(_ action: (() -> Void)?) -> some View {
            if let action {
                Button(action: action) { contentShape(Rectangle()) }.buttonStyle(.plain)
            } else {
                self
            }
        }

        @ViewBuilder
        fileprivate func nodeStyle(_ style: Style?, _ a11y: A11y?, _ theme: NodeTheme, onTap: (() -> Void)?) -> some View {
            let styled = padding(style?.padding.map(theme.space) ?? 0)
                .background(
                    style?.background.map(theme.color) ?? Color.clear,
                    in: RoundedRectangle(cornerRadius: style?.corner.map(theme.space) ?? 0)
                )
                .nodeTap(onTap)
                .accessibilityAddTraits(a11y?.heading == true ? .isHeader : [])
                .accessibilityHidden(a11y?.hidden == true)
            if let label = a11y?.label {
                styled.accessibilityLabel(label)
            } else {
                styled
            }
        }
    }

    private extension NodeEvent {
        var known: Bool {
            if case .unknown = self { return false }
            return true
        }
    }

    private extension HAlign {
        var alignment: HorizontalAlignment {
            switch self {
            case .start: return .leading
            case .center: return .center
            case .end: return .trailing
            }
        }
    }

    private extension VAlign {
        var alignment: VerticalAlignment {
            switch self {
            case .top: return .top
            case .center: return .center
            case .bottom: return .bottom
            }
        }
    }

    private extension InputKind {
        var keyboard: UIKeyboardType {
            switch self {
            case .text, .password: return .default
            case .email: return .emailAddress
            case .number: return .numberPad
            }
        }

        var contentType: UITextContentType? {
            switch self {
            case .text, .number: return nil
            case .email: return .emailAddress
            case .password: return .password
            }
        }
    }
""".trimIndent() + "\n"
