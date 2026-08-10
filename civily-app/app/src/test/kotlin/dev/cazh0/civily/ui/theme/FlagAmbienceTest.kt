package dev.cazh0.civily.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FlagAmbienceTest {

    @Test
    fun `an empty image keeps the caller's surface`() {
        assertEquals(DARK_SURFACE, FlagAmbience.of(IntArray(0), DARK_SURFACE))
    }

    @Test
    fun `a fully transparent image keeps the caller's surface`() {
        assertEquals(DARK_SURFACE, FlagAmbience.of(IntArray(64) { TRANSPARENT }, DARK_SURFACE))
    }

    @Test
    fun `a flag too sparse to read anything from keeps the caller's surface`() {
        // One opaque pixel in 256 is a decode artefact, not artwork.
        val oneSpeck = IntArray(256) { if (it == 0) RED else TRANSPARENT }

        assertEquals(DARK_SURFACE, FlagAmbience.of(oneSpeck, DARK_SURFACE))
    }

    @Test
    fun `an opaque flag gets a frame that stays with the theme`() {
        // The defect the device showed: a fully opaque red flag was given a pale pink slab,
        // which is the bright-box problem again in a new colour. Nothing is read through an
        // opaque flag, so its plate follows the surface.
        val plate = FlagAmbience.of(IntArray(64) { RED }, DARK_SURFACE)

        assertTrue(
            "plate ${plate.luminance()} should sit near the dark surface",
            abs(plate.luminance() - DARK_SURFACE.luminance()) < NEAR,
        )
    }

    @Test
    fun `an opaque flag still tints its frame`() {
        // Near the surface, but not the surface: a red flag's frame is a warmer dark than a
        // blue flag's. That difference is the whole effect.
        val warm = FlagAmbience.of(IntArray(64) { RED }, DARK_SURFACE)
        val cool = FlagAmbience.of(IntArray(64) { BLUE }, DARK_SURFACE)

        assertTrue(warm.red > cool.red)
        assertTrue(cool.blue > warm.blue)
    }

    @Test
    fun `an opaque flag on a light theme gets a light frame`() {
        val plate = FlagAmbience.of(IntArray(64) { RED }, LIGHT_SURFACE)

        assertTrue(plate.luminance() > DARK_SURFACE.luminance())
    }

    @Test
    fun `black artwork on a transparent sheet is given something to be read against`() {
        // Legibility outranks blending: this stays pale even on a dark surface, because the
        // alternative is a flag nobody can see.
        val blackOnGlass = IntArray(64) { if (it < 32) BLACK else TRANSPARENT }

        val plate = FlagAmbience.of(blackOnGlass, DARK_SURFACE)

        assertTrue(plate.luminance() > MIN_READABLE_LUMINANCE)
    }

    @Test
    fun `white artwork on a transparent sheet is given a dark plate`() {
        val whiteOnGlass = IntArray(64) { if (it < 32) WHITE else TRANSPARENT }

        val plate = FlagAmbience.of(whiteOnGlass, LIGHT_SURFACE)

        assertTrue(plate.luminance() < LIGHT_SURFACE.luminance())
    }

    @Test
    fun `transparent pixels do not drag the average`() {
        // A flag that is one red stripe on a transparent sheet must read as red, not as a
        // red-and-nothing mix biased toward black.
        val stripeOnGlass = IntArray(64) { if (it < 24) RED else TRANSPARENT }

        val plate = FlagAmbience.of(stripeOnGlass, DARK_SURFACE)

        assertTrue(plate.red > plate.green)
        assertTrue(plate.red > plate.blue)
    }

    private companion object {
        val DARK_SURFACE = Color(0xFF1E1E1E)
        val LIGHT_SURFACE = Color(0xFFECECEC)

        const val TRANSPARENT = 0x00000000
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
        const val RED = 0xFFCC0000.toInt()
        const val BLUE = 0xFF0033CC.toInt()

        /** Close enough to the surface that the plate reads as chrome, not as a second image. */
        const val NEAR = 0.05f

        /** Comfortably above the luminance of anything that would hide black artwork. */
        const val MIN_READABLE_LUMINANCE = 0.25f
    }
}
