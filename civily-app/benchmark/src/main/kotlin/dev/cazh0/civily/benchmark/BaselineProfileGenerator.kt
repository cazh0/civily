package dev.cazh0.civily.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the Baseline Profile the app ships with.
 *
 * What a profile is: a list of the methods and classes a first session actually runs. Android
 * compiles those ahead of time at install, so the code that draws the first screen is native
 * before it is reached instead of being interpreted, then JIT'd, while the user watches. For a
 * Compose app that is most of cold start and a large share of the first frames of every screen,
 * because the Compose runtime itself is the code being interpreted.
 *
 * Why it is recorded from a journey rather than written by hand: the profile is only worth what
 * its accuracy is worth. A guessed list of hot classes is a guess shipped as fact (spec §1.1),
 * and a wrong one wastes install-time compilation on code nobody runs.
 *
 * Run it with:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * which installs the app, walks [openNation] and [readEveryTab] on the connected device, and
 * writes the result to `app/src/release/generated/baselineProfiles/`. That file is checked in:
 * it is a build input, and a build whose output depends on a device being plugged in is not a
 * build.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    /**
     * Cold start and nothing else.
     *
     * This one is marked as a startup profile, which is a second use of the same recording: the
     * classes it names are laid next to each other in the dex file, so starting the app reads one
     * run of pages instead of seeking across the file. That only helps if the list is *short* —
     * a startup profile naming everything the app can do orders the dex by nothing at all, which
     * is why the journey below is deliberately excluded from it.
     */
    @Test
    fun startup() = rule.collect(
        packageName = CIVILY,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Everything a first session does after it has started: find a nation, open it, read every
     * subject, come back.
     *
     * Ahead-of-time compiled like the rest of the profile, but kept out of the startup ordering
     * for the reason above.
     */
    @Test
    fun nation() = rule.collect(
        packageName = CIVILY,
        includeInStartupProfile = false,
    ) {
        pressHome()
        startActivityAndWait()

        openNation()
        readEveryTab()

        // The way back out is code too, and it is the transition the user notices most.
        device.pressBack()
        device.waitForIdle()
    }
}
