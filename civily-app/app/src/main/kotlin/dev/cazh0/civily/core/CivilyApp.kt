package dev.cazh0.civily.core

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Why [onCreate] does nothing but hand one object to a background thread: everything here runs
 * before the first frame and is charged directly against the §4 cold-start budget. [graph]
 * builds on first touch, and every object in it is lazy for the same reason.
 *
 * The one exception is the session store, and it earns the exception by being the one lazy
 * object that reads a file. `getSharedPreferences` opens, parses and caches an XML document, and
 * the first thing to ask for it is the composition of the first screen — so the read lands on
 * the main thread, inside the first frame, which is exactly what spec §2 R2b forbids. Started
 * here it overlaps the Activity's own creation and is finished before anything asks. `by lazy`
 * is synchronised, so a screen that somehow gets there first waits for the read rather than
 * repeating it, and cannot see a half-built store either way.
 */
class CivilyApp : Application() {

    val graph: AppGraph by lazy { AppGraph(this) }

    /**
     * Lives as long as the process, which is the honest scope for warming a process-wide object.
     * Nothing here is ever cancelled because nothing here has a consumer to disappoint.
     */
    private val startup = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startup.launch { graph.sessionStore }
    }
}
