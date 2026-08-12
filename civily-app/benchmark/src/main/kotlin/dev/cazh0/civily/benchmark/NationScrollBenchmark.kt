package dev.cazh0.civily.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every frame of the nation screen, measured — spec §4's "60fps, zero jank frames", which
 * nothing checked before.
 *
 * [FrameTimingMetric] reports the duration of each frame the app produced. What matters is the
 * high percentiles: a median inside the budget with a 99th percentile outside it is a scroll
 * that stutters, and an average hides exactly that. On a 120Hz display the budget is 8.3ms.
 *
 * The whole journey is measured rather than one fling, because the panes are where the work is:
 * seven tabs, a ninety-row list, a chart, and a feed of parsed markup.
 */
@RunWith(AndroidJUnit4::class)
class NationScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollEveryTab() = rule.measureRepeated(
        packageName = CIVILY,
        metrics = listOf(FrameTimingMetric()),
        // The shipped app, compiled the way it installs. Measuring an interpreted build would be
        // measuring something nobody runs.
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
        ),
        // Why no `startupMode`: it kills the app *between* the setup and the measured block, so
        // a setup that navigates somewhere hands the measurement an app that is no longer there
        // — measuring the launcher instead of the nation screen. Startup is [StartupBenchmark]'s
        // question; this one is about the frames after it, so the kill is done here, before the
        // navigation rather than after it.
        iterations = ITERATIONS,
        setupBlock = {
            killProcess()
            pressHome()
            startActivityAndWait()
            openNation()
        },
    ) {
        readEveryTab()
    }

    private companion object {
        /** Each iteration is a cold start and seven tabs over the network. Three is minutes. */
        const val ITERATIONS = 3
    }
}
