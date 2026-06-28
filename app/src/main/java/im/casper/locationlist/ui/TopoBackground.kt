// app/src/main/java/im/casper/locationlist/ui/TopoBackground.kt
package im.casper.locationlist.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.floor

private data class TopoPalette(val background: Color, val minor: Color, val major: Color)

private val DarkTopo = TopoPalette(
    background = Color(0xFF101012),
    minor = Color(0xFF18181B),
    major = Color(0xFF212126),
)
private val LightTopo = TopoPalette(
    background = Color(0xFFF6EFE2),
    minor = Color(0xFFECE2D0),
    major = Color(0xFFE3D7C0),
)

/**
 * A topographic-contour background. Fills its area, draws contour lines, and places [content]
 * on top. The palette follows the active theme automatically (dark vs light), or you can force
 * it with [darkTheme]. The pattern is deterministic for a given [seed] and size.
 */
@Composable
fun TopoBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    seed: Int = 7,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val palette = if (darkTheme) DarkTopo else LightTopo
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val cellPx = with(density) { 26.dp.toPx() }

        // Build the contour geometry once per size/seed; recolor cheaply on theme change.
        val paths = remember(wPx, hPx, seed, cellPx) {
            buildContours(wPx, hPx, seed, cellPx)
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            drawPath(paths.first, color = palette.minor, style = Stroke(width = 1.dp.toPx()))
            drawPath(paths.second, color = palette.major, style = Stroke(width = 1.6.dp.toPx()))
        }

        content()
    }
}

// --- Contour generation: value-noise heightfield + marching squares ------------------------

private fun buildContours(
    w: Float,
    h: Float,
    seed: Int,
    cell: Float,
    levels: Int = 14,
): Pair<Path, Path> {
    val minor = Path()
    val major = Path()
    if (w <= 0f || h <= 0f) return minor to major

    val cols = (w / cell).toInt() + 2
    val rows = (h / cell).toInt() + 2
    val freq = 0.10f // smaller = larger, broader "hills"

    val height = Array(rows) { r -> FloatArray(cols) { c -> fbm(c * freq, r * freq, seed) } }

    var mn = Float.MAX_VALUE
    var mx = -Float.MAX_VALUE
    for (r in 0 until rows) for (c in 0 until cols) {
        val v = height[r][c]
        if (v < mn) mn = v
        if (v > mx) mx = v
    }
    val range = (mx - mn).coerceAtLeast(1e-4f)

    for (i in 0 until levels) {
        val level = mn + range * (i + 0.5f) / levels
        val target = if (i % 4 == 0) major else minor
        for (r in 0 until rows - 1) {
            for (c in 0 until cols - 1) {
                val x0 = c * cell; val x1 = x0 + cell
                val y0 = r * cell; val y1 = y0 + cell
                val tl = height[r][c]; val tr = height[r][c + 1]
                val br = height[r + 1][c + 1]; val bl = height[r + 1][c]

                var m = 0
                if (tl > level) m = m or 8
                if (tr > level) m = m or 4
                if (br > level) m = m or 2
                if (bl > level) m = m or 1
                if (m == 0 || m == 15) continue

                fun top() = Offset(x0 + (x1 - x0) * ((level - tl) / (tr - tl)), y0)
                fun right() = Offset(x1, y0 + (y1 - y0) * ((level - tr) / (br - tr)))
                fun bottom() = Offset(x1 + (x0 - x1) * ((level - br) / (bl - br)), y1)
                fun left() = Offset(x0, y1 + (y0 - y1) * ((level - bl) / (tl - bl)))
                fun seg(a: Offset, b: Offset) {
                    target.moveTo(a.x, a.y)
                    target.lineTo(b.x, b.y)
                }

                when (m) {
                    1, 14 -> seg(left(), bottom())
                    2, 13 -> seg(bottom(), right())
                    3, 12 -> seg(left(), right())
                    4, 11 -> seg(top(), right())
                    6, 9 -> seg(top(), bottom())
                    7, 8 -> seg(top(), left())
                    5 -> { seg(top(), left()); seg(bottom(), right()) }
                    10 -> { seg(top(), right()); seg(left(), bottom()) }
                }
            }
        }
    }
    return minor to major
}

private fun fbm(fx: Float, fy: Float, seed: Int): Float {
    var amp = 0.5f
    var freq = 1f
    var sum = 0f
    var norm = 0f
    for (o in 0 until 4) {
        sum += amp * valueNoise(fx * freq, fy * freq, seed + o * 101)
        norm += amp
        amp *= 0.5f
        freq *= 2f
    }
    return sum / norm
}

private fun valueNoise(fx: Float, fy: Float, seed: Int): Float {
    val x0 = floor(fx).toInt()
    val y0 = floor(fy).toInt()
    val tx = smooth(fx - x0)
    val ty = smooth(fy - y0)
    val v00 = hash(x0, y0, seed)
    val v10 = hash(x0 + 1, y0, seed)
    val v01 = hash(x0, y0 + 1, seed)
    val v11 = hash(x0 + 1, y0 + 1, seed)
    val a = v00 + (v10 - v00) * tx
    val b = v01 + (v11 - v01) * tx
    return a + (b - a) * ty
}

private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

private fun hash(x: Int, y: Int, seed: Int): Float {
    var h = x * 374761393 + y * 668265263 + seed * 362437
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0x7fffffff) / 2147483647f
}
