package dev.cazh0.civily.core

import android.app.Application

/**
 * Why there is no `onCreate` override: work done here runs before the first frame and is
 * charged directly against the §4 cold-start budget. [graph] builds on first touch instead.
 */
class CivilyApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
