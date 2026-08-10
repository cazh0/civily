package dev.cazh0.civily.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The colour a flag should sit on, taken from the flag itself.
 *
 * NationStates flags are authored against the white page of the website. A fixed light plate
 * keeps every one of them legible and makes the app a slab of grey at night; a theme-coloured
 * plate looks right and swallows the transparent PNGs whose artwork is black. Neither is a
 * choice worth making globally, because the right plate is a property of the picture.
 *
 * What the picture decides is *which question is being asked*, and there are two.
 *
 * An opaque flag covers its plate. Nothing has to be read against it, so the plate is only a
 * frame, and the quietest good frame is the app's own surface carrying a wash of the flag's
 * colour — the flag bleeding outward. Following the surface is also what keeps it dark in a
 * dark theme, which a fixed light grey never was.
 *
 * A flag with transparency shows its plate *through* the artwork, so now the plate has a job:
 * be the thing that artwork is legible against. Its average colour is the artwork's own colour,
 * so pushing away from that brightness lands the plate on the far side of it. Legibility wins
 * over blending here; a black-on-glass flag gets a pale plate even at night, because the
 * alternative is a flag nobody can see.
 *
 * Pure on purpose: pixels in, colour out. That is what makes it testable without a device
 * (spec §5), and the Android side of it is nothing but reading a grid of pixels.
 */
object FlagAmbience {

    /**
     * @param pixels ARGB pixels sampled anywhere in the image; order is irrelevant.
     * @param surface the plate the app would use with no flag to go on. Returned outright when
     *   there is too little artwork to conclude anything from, and blended into the answer for
     *   a flag that needs a frame rather than a backdrop.
     */
    fun of(pixels: IntArray, surface: Color): Color {
        if (pixels.isEmpty()) return surface

        var alphaTotal = 0L
        var red = 0L
        var green = 0L
        var blue = 0L

        for (pixel in pixels) {
            val alpha = (pixel ushr ALPHA_SHIFT) and CHANNEL_MASK
            alphaTotal += alpha
            // Weighted by alpha so a transparent margin cannot drag the average toward a
            // colour that is not actually painted anywhere.
            red += alpha * ((pixel ushr RED_SHIFT) and CHANNEL_MASK)
            green += alpha * ((pixel ushr GREEN_SHIFT) and CHANNEL_MASK)
            blue += alpha * ((pixel ushr BLUE_SHIFT) and CHANNEL_MASK)
        }

        val coverage = alphaTotal.toFloat() / (pixels.size * CHANNEL_MASK)
        if (coverage < MIN_COVERAGE) return surface

        val mean = Color(
            red = (red / alphaTotal).toInt(),
            green = (green / alphaTotal).toInt(),
            blue = (blue / alphaTotal).toInt(),
        )

        // Nothing shows through, so the plate is a frame: the app's surface, warmed by the
        // flag. Mostly surface, because a frame that competes with the picture is not a frame.
        if (coverage >= OPAQUE) return mean.blend(surface, TOWARD_SURFACE)

        // Artwork sits directly on the plate, so the plate has to be the far side of it:
        // toward black for bright artwork, toward white for dark. The flag's hue survives the
        // blend, which is what stops it reading as a swatch from a palette.
        val target = if (mean.luminance() > MID_LUMINANCE) Color.Black else Color.White
        return mean.blend(target, SHIFT)
    }

    private fun Color.blend(other: Color, amount: Float) = Color(
        red = red + (other.red - red) * amount,
        green = green + (other.green - green) * amount,
        blue = blue + (other.blue - blue) * amount,
    )

    private fun Color(red: Int, green: Int, blue: Int) =
        Color(red = red / CHANNEL_MAX, green = green / CHANNEL_MAX, blue = blue / CHANNEL_MAX)

    /**
     * Below this, the image is essentially empty — a decoding failure or a placeholder. There
     * is nothing to be ambient about, so the caller's theme colour stands.
     */
    private const val MIN_COVERAGE = 0.05f

    /**
     * Above this the flag is treated as covering its plate. Not 1.0: a flag with a rounded
     * corner or a hairline of antialiasing is still an opaque flag, and nothing is being read
     * through those few pixels.
     */
    private const val OPAQUE = 0.85f

    /**
     * How much of the frame is the app's surface rather than the flag. Mostly surface: enough
     * of the flag to feel like its own, not enough to become a second picture.
     */
    private const val TOWARD_SURFACE = 0.78f

    /**
     * How far the plate moves from the artwork. Enough that black-on-transparent stays
     * readable; not so far that the plate turns into the flat white it replaces.
     */
    private const val SHIFT = 0.62f

    /**
     * Perceptual midpoint rather than 0.5: `luminance` is linear, and linear 0.18 is where a
     * mid grey sits to the eye. Splitting at 0.5 calls most flags dark and pushes their plates
     * white, which is the very thing this exists to stop.
     */
    private const val MID_LUMINANCE = 0.18f

    private const val CHANNEL_MASK = 0xFF
    private const val CHANNEL_MAX = 255f
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0
}
