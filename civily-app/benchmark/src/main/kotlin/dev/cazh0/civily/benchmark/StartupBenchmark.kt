package dev.cazh0.civily.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start, measured — which is the first thing in spec §4 that nothing measured before.
 *
 * Two tests rather than one, because a single number answers nothing: [coldStartNoProfile] is
 * the app as it installs with no ahead-of-time compilation at all, and [coldStartWithProfile] is
 * the app as it ships. The difference between them is what the Baseline Profile is worth on this
 * device, and it is the only honest way to state that figure.
 *
 * Run with:
 * ```
 * ./gradlew :benchmark:connectedBenchmarkAndroidTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartNoProfile() = measure(CompilationMode.None())

    @Test
    fun coldStartWithProfile() = measure(
        // Require, not Enable: this fails rather than quietly measuring an unprofiled app if the
        // profile is missing from the APK. A benchmark that cannot tell you it measured nothing
        // is worse than no benchmark.
        CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
    )

    private fun measure(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = CIVILY,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }

    private companion object {
        /** Enough runs for the median to mean something, few enough to sit through. */
        const val ITERATIONS = 10
    }
}
