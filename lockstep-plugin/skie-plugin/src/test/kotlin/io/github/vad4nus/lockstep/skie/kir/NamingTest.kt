package io.github.vad4nus.lockstep.skie.kir

import io.github.vad4nus.lockstep.skie.render.swiftIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals

class NamingTest {

    @Test
    fun caseNamesAreLowerCamelSimpleNames() {
        assertEquals("error", caseName("io.sample.ProfileScreenState.Error"))
        assertEquals("showToast", caseName("io.sample.ProfileAction.ShowToast"))
        assertEquals("urlLoaded", caseName("io.sample.Event.URLLoaded"))
        assertEquals("http", caseName("io.sample.Event.HTTP"))
        assertEquals("a", caseName("io.sample.Event.A"))
        assertEquals("already", caseName("io.sample.Event.already"))
    }

    @Test
    fun replacedKotlinNamesKeepNestingAndPrefixOnlyReplacedParts() {
        assertEquals("__ProfileState", kotlinSwiftName("ProfileState", listOf(true)))
        assertEquals("__NoInternetAction.ToLoginScreen", kotlinSwiftName("NoInternetAction.ToLoginScreen", listOf(true, false)))
        assertEquals("Outer.__Inner", kotlinSwiftName("Outer.Inner", listOf(false, true)))
        assertEquals("ProfileScreenStateError", kotlinSwiftName("ProfileScreenStateError", listOf(true, false)))
        assertEquals("__OuterInner", kotlinSwiftName("OuterInner", listOf(false, true)))
    }

    @Test
    fun generatedNamesFlattenNesting() {
        assertEquals("OuterInner", generatedSwiftName("Outer.Inner"))
        assertEquals("Address", generatedSwiftName("Address"))
    }

    @Test
    fun keywordsAreBackticked() {
        assertEquals("`default`", swiftIdentifier("default"))
        assertEquals("`class`", swiftIdentifier("class"))
        assertEquals("name", swiftIdentifier("name"))
    }

    @Test
    fun duplicateNamesAreDetected() {
        assertEquals(setOf("Address"), duplicates(listOf("Address", "Profile", "Address")))
        assertEquals(emptySet(), duplicates(listOf("Address", "Profile")))
    }
}
