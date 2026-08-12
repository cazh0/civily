package dev.cazh0.civily.ui.component

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.cazh0.civily.ui.theme.FlagAmbience
import dev.cazh0.civily.ui.theme.Motion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A flag on a plate mixed from its own colours.
 *
 * The plate starts as the theme's surface and settles onto the flag's ambience once the image
 * has decoded, so a flag that is slow to arrive never flashes a colour it does not have.
 * [FlagAmbience] decides the colour; everything here is about getting it the pixels without
 * touching the main thread.
 */
@Composable
fun AmbientFlag(
    flagUrl: String,
    contentDescription: String?,
    imageLoader: ImageLoader,
    shape: Shape,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    contentPadding: Dp = Dp.Hairline,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val context = LocalContext.current

    // Why the request is built here rather than passing the url straight to AsyncImage: Coil
    // decodes to Bitmap.Config.HARDWARE from API 26, and a hardware bitmap lives in graphics
    // memory with no CPU-readable pixels — reading one throws, on every device that has ever
    // run this. The plate is mixed from those pixels, so the decode has to be told to produce
    // something readable. It is asked for here, in the one place that samples, rather than
    // guarded for afterwards. Flags cost tens of kilobytes; the copy is not worth measuring.
    val request = remember(flagUrl) {
        ImageRequest.Builder(context)
            .data(flagUrl)
            .allowHardware(false)
            .build()
    }

    // Keyed on the url so switching nations never shows the previous flag's plate.
    var ambience by remember(flagUrl) { mutableStateOf(surface) }
    val scope = rememberCoroutineScope()

    val plate by animateColorAsState(
        targetValue = ambience,
        // A tween rather than the default spring: a spring settles over an unbounded number of
        // frames chasing a difference nobody can see, and this animation is decoration on a
        // picture that has already arrived.
        animationSpec = tween(Motion.PlateMillis),
        label = "flag ambience",
    )

    Surface(shape = shape, color = plate, modifier = modifier) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            // Why the bitmap is sampled here and not held in state: keeping it would pin a full
            // decoded image in the heap for as long as the screen exists, on top of the copy
            // Coil's own cache is already holding — and this composable needs one colour out of
            // it, once. Sampling is arithmetic over a few hundred pixels, but it is still
            // reading an image, so it goes to a background dispatcher (spec §3); the reference
            // dies with the coroutine.
            onSuccess = { state ->
                val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    scope.launch {
                        ambience = withContext(Dispatchers.Default) {
                            FlagAmbience.of(bitmap.grid(), surface)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

/**
 * An evenly spaced grid of pixels, small and fixed so the cost does not depend on how large a
 * flag the region happens to fly.
 *
 * Why a grid rather than a scaled copy: scaling allocates a second bitmap for an answer that is
 * an average either way.
 */
private fun Bitmap.grid(): IntArray {
    val columns = minOf(SAMPLES_PER_AXIS, width)
    val rows = minOf(SAMPLES_PER_AXIS, height)
    if (columns <= 0 || rows <= 0) return IntArray(0)

    val pixels = IntArray(columns * rows)
    for (row in 0 until rows) {
        val y = row * height / rows
        for (column in 0 until columns) {
            pixels[row * columns + column] = getPixel(column * width / columns, y)
        }
    }
    return pixels
}

/** 16×16 is 256 samples: past the point where more of them change the average. */
private const val SAMPLES_PER_AXIS = 16
