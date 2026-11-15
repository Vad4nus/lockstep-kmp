@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.vad4nus.lockstep.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.lockstep.mvvm.navigation.StackNavigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigatorDisplayTest {

    private data object Counter : Route
    private data object Other : Route

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aRepeatedRouteGetsAContentKeyOfItsOwn() {
        val keys = stackEntries(listOf(Counter, Other, Counter, Counter)) { NavEntry(it) {} }.map { it.contentKey }

        assertEquals(NavEntry<Route>(Counter) {}.contentKey, keys[0])
        assertEquals(4, keys.toSet().size)
        assertNotEquals(keys[0], keys[2])
    }

    @Test
    fun equalRoutesInTheStackKeepSeparateState() {
        val navigator = StackNavigator(Counter)
        rule.setContent {
            NavigatorDisplay(navigator) { route ->
                NavEntry(route) {
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        Text("count $count")
                        Button({ count++ }) { Text("add") }
                    }
                }
            }
        }

        rule.onNodeWithText("add").performClick()
        rule.onNodeWithText("count 1").assertIsDisplayed()

        rule.runOnIdle { navigator.push(Counter) }
        rule.onNodeWithText("count 0").assertIsDisplayed()

        rule.runOnIdle { navigator.pop() }
        rule.onNodeWithText("count 1").assertIsDisplayed()
    }
}
