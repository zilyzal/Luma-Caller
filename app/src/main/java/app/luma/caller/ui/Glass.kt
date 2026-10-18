package app.luma.caller.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LumaBg = Color(0xFF070B1F)
val SimGreen = listOf(Color(0xF246EBA0), Color(0xE619B97D))
val SimBlue = listOf(Color(0xF250C8FF), Color(0xEB2D78EB))
val DangerRed = listOf(Color(0xF5FF5F6E), Color(0xEBE13250))
val Accent = listOf(Color(0xE65A8CFF), Color(0xD99A5CFF))
val SoftBlue = Color(0xFF9FDCFF)
val MissedPink = Color(0xFFFF8FA3)

@Composable
fun LumaBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(LumaBg)
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x805059FF), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = w * 1.25f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x6100BEB9), Color.Transparent),
                        center = Offset(w, h),
                        radius = w * 1.15f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x47A050FF), Color.Transparent),
                        center = Offset(w, h * 0.35f),
                        radius = w * 0.9f
                    )
                )
            },
        content = content
    )
}

/** Liquid-glass surface: translucent fill, soft highlight and a bright top edge. */
fun Modifier.glass(shape: Shape, fill: Float = 0.14f, base: Float = 0f): Modifier = this
    .clip(shape)
    .background(Color(0xFF0A0F2E).copy(alpha = base))
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = fill + 0.07f), Color.White.copy(alpha = fill - 0.03f))
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.12f))),
        shape
    )

fun Modifier.gradientFill(colors: List<Color>, shape: Shape): Modifier = this
    .clip(shape)
    .background(Brush.linearGradient(colors))
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.2f))),
        shape
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "press")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (pressed) 0.85f else 1f
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

@Composable
fun LText(
    text: String,
    size: Int = 16,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
    spacing: Float = 0f
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            textAlign = align,
            letterSpacing = spacing.sp
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

object LIcon {
    const val Call = "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z"
    const val Clock = "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18z M12 7v5l3 2"
    const val Message = "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"
    const val Person = "M12 4a4 4 0 1 1 0 8a4 4 0 1 1 0-8z M4 21c1.5-4 5-6 8-6s6.5 2 8 6"
    const val Search = "M11 4a7 7 0 1 1 0 14a7 7 0 1 1 0-14z M20 20l-3.5-3.5"
    const val Backspace = "M21 5H9l-6 7 6 7h12a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1z M13 9.5l5 5 M18 9.5l-5 5"
    const val Mic = "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z M5 11a7 7 0 0 0 14 0 M12 18v3"
    const val MicOff = "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z M5 11a7 7 0 0 0 14 0 M12 18v3 M4 4l16 16"
    const val Speaker = "M4 9v6h4l5 4V5L8 9H4z M16.5 8.5a5 5 0 0 1 0 7 M19 6a8.5 8.5 0 0 1 0 12"
    const val Plus = "M12 5v14 M5 12h14"
    const val Send = "M12 19V5 M5 12l7-7 7 7"
    const val Back = "M15 5l-7 7 7 7"
    const val Compose = "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"
    const val Info = "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18z M12 11v5 M12 8h.01"
    const val Snooze = "M12 5a8 8 0 1 1 0 16a8 8 0 1 1 0-16z M12 9v4l2.5 1.5 M5 4L3 6 M19 4l2 2"
    const val Close = "M6 6l12 12 M18 6L6 18"
}

@Composable
fun LumaIcon(
    path: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
    strokeWidth: Float = 1.8f
) {
    val vector: ImageVector = remember(path, strokeWidth) {
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()
    }
    Image(
        imageVector = vector,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

@Composable
fun DotsIcon(modifier: Modifier = Modifier, dp: Dp = 24.dp, tint: Color = Color.White) {
    Canvas(modifier.size(dp)) {
        val unit = this.size.width / 24f
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(tint, 1.7f * unit, Offset((6 + 6 * col) * unit, (6 + 6 * row) * unit))
            }
        }
    }
}

/** Round glass button holding an icon. */
@Composable
fun GlassCircleButton(
    iconPath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    iconSize: Dp = 30.dp,
    active: Boolean = false
) {
    Box(
        modifier
            .size(size)
            .glass(CircleShape, fill = if (active) 0.30f else 0.14f)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LumaIcon(iconPath, size = iconSize)
    }
}

@Composable
fun FillBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize(), content = content)
}
