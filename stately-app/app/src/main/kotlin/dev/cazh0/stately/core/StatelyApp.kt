package dev.cazh0.stately.core

import android.app.Application

/**
 * Why there is no `onCreate` override: work done here runs before the first frame and is
 * charged directly against the §4 cold-start budget. [graph] builds on first touch instead.
 */
class StatelyApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
