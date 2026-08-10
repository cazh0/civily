package dev.cazh0.civily.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.cazh0.civily.nav.CivilyNavHost
import dev.cazh0.civily.ui.theme.CivilyTheme

/**
 * The app's only Activity.
 *
 * Why one: the legacy app's fragment-state crashes (spec §5) came from state living in
 * Activity and Fragment lifecycles that outlived the callbacks writing to them. With
 * navigation inside a single composition there is one lifecycle to reason about.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Why before super: content must be laid out behind the status and navigation bars
        // from the first frame, or the app visibly reflows as it starts. Every Scaffold in
        // the app consumes the resulting insets, so nothing ends up underneath them.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CivilyTheme {
                CivilyNavHost()
            }
        }
    }
}
