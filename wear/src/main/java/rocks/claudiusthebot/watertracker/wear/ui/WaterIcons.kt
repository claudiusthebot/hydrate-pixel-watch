package rocks.claudiusthebot.watertracker.wear.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-rolled drink-size icons. Using material-icons-extended would balloon
 * the dex to 40+ MB because it brings the full Material catalogue; we only
 * need four glyphs, so they live here as compact ImageVectors.
 *
 * The four sizes: espresso cup → mug → glass → bottle.
 */
object WaterIcons {

    val Espresso: ImageVector by lazy {
        ImageVector.Builder(
            name = "Espresso",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Tiny cup with saucer
                moveTo(7f, 9f)
                lineTo(17f, 9f)
                lineTo(16.5f, 16f)
                curveTo(16.4f, 17.1f, 15.4f, 18f, 14.3f, 18f)
                lineTo(9.7f, 18f)
                curveTo(8.6f, 18f, 7.6f, 17.1f, 7.5f, 16f)
                close()
                moveTo(5f, 19f)
                lineTo(19f, 19f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                close()
            }
        }.build()
    }

    val Mug: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mug",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Cup body with handle
                moveTo(4f, 6f)
                lineTo(16f, 6f)
                lineTo(15.5f, 18f)
                curveTo(15.45f, 19.1f, 14.5f, 20f, 13.4f, 20f)
                lineTo(6.6f, 20f)
                curveTo(5.5f, 20f, 4.55f, 19.1f, 4.5f, 18f)
                close()
                moveTo(17f, 9f)
                lineTo(19f, 9f)
                curveTo(20.1f, 9f, 21f, 9.9f, 21f, 11f)
                lineTo(21f, 14f)
                curveTo(21f, 15.1f, 20.1f, 16f, 19f, 16f)
                lineTo(17f, 16f)
                lineTo(17f, 14f)
                lineTo(19f, 14f)
                lineTo(19f, 11f)
                lineTo(17f, 11f)
                close()
            }
        }.build()
    }

    val Glass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Glass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Tapered tumbler
                moveTo(6f, 4f)
                lineTo(18f, 4f)
                lineTo(17f, 20f)
                curveTo(16.95f, 21.1f, 16f, 22f, 14.9f, 22f)
                lineTo(9.1f, 22f)
                curveTo(8f, 22f, 7.05f, 21.1f, 7f, 20f)
                close()
                // waterline accent
                moveTo(8f, 8f)
                lineTo(16f, 8f)
                lineTo(15.85f, 10.5f)
                lineTo(8.15f, 10.5f)
                close()
            }
        }.build()
    }

    val Bottle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bottle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Neck
                moveTo(10f, 2f)
                lineTo(14f, 2f)
                lineTo(14f, 6f)
                lineTo(10f, 6f)
                close()
                // Body
                moveTo(8f, 8f)
                lineTo(16f, 8f)
                lineTo(16f, 21f)
                curveTo(16f, 21.55f, 15.55f, 22f, 15f, 22f)
                lineTo(9f, 22f)
                curveTo(8.45f, 22f, 8f, 21.55f, 8f, 21f)
                close()
            }
        }.build()
    }

    val Drop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Drop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3f)
                curveTo(12f, 3f, 6f, 11f, 6f, 15f)
                curveTo(6f, 18.3f, 8.7f, 21f, 12f, 21f)
                curveTo(15.3f, 21f, 18f, 18.3f, 18f, 15f)
                curveTo(18f, 11f, 12f, 3f, 12f, 3f)
                close()
            }
        }.build()
    }

    val Plus = lazy {
        ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(11f, 5f)
                lineTo(13f, 5f)
                lineTo(13f, 11f)
                lineTo(19f, 11f)
                lineTo(19f, 13f)
                lineTo(13f, 13f)
                lineTo(13f, 19f)
                lineTo(11f, 19f)
                lineTo(11f, 13f)
                lineTo(5f, 13f)
                lineTo(5f, 11f)
                lineTo(11f, 11f)
                close()
            }
        }.build()
    }.value

    val Minus = lazy {
        ImageVector.Builder(
            name = "Minus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 11f)
                lineTo(19f, 11f)
                lineTo(19f, 13f)
                lineTo(5f, 13f)
                close()
            }
        }.build()
    }.value

    val Check = lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 16.2f)
                lineTo(4.8f, 12f)
                lineTo(3.4f, 13.4f)
                lineTo(9f, 19f)
                lineTo(21f, 7f)
                lineTo(19.6f, 5.6f)
                close()
            }
        }.build()
    }.value

    val Close = lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 6.41f)
                lineTo(17.59f, 5f)
                lineTo(12f, 10.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(12f, 13.41f)
                lineTo(17.59f, 19f)
                lineTo(19f, 17.59f)
                lineTo(13.41f, 12f)
                close()
            }
        }.build()
    }.value
}
