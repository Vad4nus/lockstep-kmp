@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.vad4nus.lockstep.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.navigation3.runtime.NavEntry
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.lockstep.mvvm.navigation.SheetRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SheetEntryTest {

    private data object List : Route
    private data class Details(val id: Int) : Route
    private data class Filter(val preset: String? = null) : Route, SheetRoute

    private val provider = sheetAwareProvider({ key -> NavEntry(key) {} }, ModalBottomSheetProperties())

    private fun entries(vararg keys: Route) = keys.map(provider)

    @Test
    fun selectsTheSheetOnTop() {
        val filter = Filter()
        assertEquals(filter, sheetEntry(entries(List, filter))?.route)
    }

    @Test
    fun selectsNothingForTheEntriesUnderTheSheet() {
        assertNull(sheetEntry(entries(List)))
    }

    @Test
    fun selectsNothingWhenAScreenIsOnTop() {
        assertNull(sheetEntry(entries(List, Details(1))))
    }
}
