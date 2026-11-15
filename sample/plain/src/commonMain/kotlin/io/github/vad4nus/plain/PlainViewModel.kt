package io.github.vad4nus.plain

import io.github.vad4nus.lockstep.UiStateFlow
import io.github.vad4nus.lockstep.asUiStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class PlainState(val title: String = "plain")

class PlainViewModel {
    private val cell = MutableStateFlow(PlainState())
    val state: UiStateFlow<PlainState> = cell.asUiStateFlow()
}
