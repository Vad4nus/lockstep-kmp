package io.github.vad4nus.sample

sealed interface SpikeNode {
    data class Column(val id: String, val children: List<SpikeNode>) : SpikeNode
    data class Card(val id: String, val content: SpikeNode) : SpikeNode
    data class Sections(val id: String, val items: List<SpikeSection>) : SpikeNode
    data class Text(val id: String, val text: String) : SpikeNode
}

data class SpikeSection(val title: String, val body: SpikeNode, val subsections: List<SpikeSection>)

data class SpikeLink(val label: String, val next: SpikeLink?)

data class SpikeScreen(val root: SpikeNode, val link: SpikeLink)

class SpikeScreenViewModel : BaseViewModel<SpikeScreen, ProfileAction>(SpikeScreen(sampleTree, SpikeLink("first", SpikeLink("second", null))))

private val sampleTree: SpikeNode = SpikeNode.Column(
    "root",
    listOf(
        SpikeNode.Text("title", "Spike"),
        SpikeNode.Card("card", SpikeNode.Column("inner", listOf(SpikeNode.Text("leaf", "Nested")))),
        SpikeNode.Sections("sections", listOf(SpikeSection("Section", SpikeNode.Text("body", "Body"), listOf(SpikeSection("Sub", SpikeNode.Text("sub", "Sub"), emptyList()))))),
    ),
)
