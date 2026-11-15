import SampleShared
import SwiftUI
import XCTest

@MainActor
final class ScreenNodesSpikeTests: XCTestCase {

    func testRecursiveTreeMapsToNativeSwift() {
        let root = SpikeScreenViewModel().state.value.root

        let sub = SpikeSection(title: "Sub", body: .text(id: "sub", text: "Sub"), subsections: [])
        XCTAssertEqual(root, .column(id: "root", children: [
            .text(id: "title", text: "Spike"),
            .card(id: "card", content: .column(id: "inner", children: [.text(id: "leaf", text: "Nested")])),
            .sections(id: "sections", items: [SpikeSection(title: "Section", body: .text(id: "body", text: "Body"), subsections: [sub])]),
        ]))
    }

    func testDataClassOnlyCycleStaysKotlinPastTheFirstLevel() {
        let link = SpikeScreenViewModel().state.value.link

        XCTAssertEqual(link.label, "first")
        XCTAssertEqual(link.next?.label, "second")
        XCTAssertNil(link.next?.next)
    }

    func testRecursiveTreeRendersThroughARecursiveView() {
        let root = SpikeScreenViewModel().state.value.root

        XCTAssertNotNil(ImageRenderer(content: SpikeNodeView(node: root)).uiImage)
    }
}

private struct SpikeNodeView: View {
    let node: SpikeNode

    var body: some View {
        switch node {
        case let .column(_, children):
            VStack { ForEach(children.indices, id: \.self) { SpikeNodeView(node: children[$0]) } }
        case let .card(_, content):
            SpikeNodeView(node: content).padding()
        case let .sections(_, items):
            ForEach(items.indices, id: \.self) { SpikeNodeView(node: items[$0].body) }
        case let .text(_, text):
            Text(text)
        }
    }
}
