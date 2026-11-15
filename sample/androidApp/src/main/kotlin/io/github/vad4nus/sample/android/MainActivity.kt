package io.github.vad4nus.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import io.github.vad4nus.lockstep.compose.NavigatorDisplay
import io.github.vad4nus.sample.orders.SampleGraph

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                NavigatorDisplay(SampleGraph.navigator, entryProvider = ordersEntryProvider)
            }
        }
    }
}
