package app.luma.caller.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
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
                // Slow-moving light behind the glass so its refraction always has something to catch.
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x33FF5FD2), Color.Transparent),
                        center = Offset(w * (0.15f + 0.5f * drift), h * (0.62f + 0.1f * drift)),
                        radius = w * 0.7f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x2E22D3EE), Color.Transparent),
                        center = Offset(w * (0.95f - 0.55f * drift), h * (0.18f + 0.25f * drift)),
                        radius = w * 0.65f
                    )
                )
            },
        content = content
    )
}

/** Specular bloom, bottom rim light and a faint colour dispersion: what makes a surface read as thick glass. */
private fun Modifier.glassHighlights(strength: Float = 1f): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    val reach = maxOf(w, h)
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.26f * strength), Color.Transparent),
            center = Offset(w * 0.22f, 0f),
            radius = reach * 0.7f
        )
    )
    drawRect(
        Brush.verticalGradient(
            0.6f to Color.Transparent,
            1f to Color.White.copy(alpha = 0.13f * strength)
        )
    )
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x2E7AE7FF), Color.Transparent),
            center = Offset(w, h),
            radius = reach * 0.6f
        )
    )
}

private val RimBrush = Brush.linearGradient(
    listOf(Color(0xCCFFFFFF), Color(0x2EB8F0FF), Color(0x14FFFFFF), Color(0x70FFC8F5))
)
private val InnerRimBrush = Brush.verticalGradient(
    0f to Color(0x59FFFFFF),
    0.35f to Color.Transparent,
    0.7f to Color.Transparent,
    1f to Color(0x24FFFFFF)
)

/** Liquid-glass surface: floating shadow, tinted body, bright specular, chromatic rim and an inner edge. */
fun Modifier.glass(shape: Shape, fill: Float = 0.14f, base: Float = 0f, depth: Float = 1f): Modifier = this
    .shadow(
        elevation = (10f * depth).dp,
        shape = shape,
        clip = false,
        ambientColor = Color(0x40000020),
        spotColor = Color(0x66101050)
    )
    .clip(shape)
    .background(Color(0xFF0A0F2E).copy(alpha = base))
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = fill + 0.08f), Color.White.copy(alpha = fill - 0.04f))
        )
    )
    .glassHighlights()
    .border(1.dp, RimBrush, shape)
    .padding(1.dp)
    .border(0.6.dp, InnerRimBrush, shape)

/** Solid-colour glass (call, decline, accent buttons) with a coloured glow underneath. */
fun Modifier.gradientFill(colors: List<Color>, shape: Shape): Modifier = this
    .shadow(
        elevation = 12.dp,
        shape = shape,
        clip = false,
        ambientColor = colors.first().copy(alpha = 0.35f),
        spotColor = colors.first().copy(alpha = 0.8f)
    )
    .clip(shape)
    .background(Brush.linearGradient(colors))
    .glassHighlights(1.15f)
    .border(1.dp, RimBrush, shape)
    .padding(1.dp)
    .border(0.6.dp, InnerRimBrush, shape)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    shape: Shape? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "press"
    )
    val haptics = LocalHapticFeedback.current
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (pressed) {
                if (shape == null) {
                    drawRect(Color.White.copy(alpha = 0.14f))
                } else {
                    val tintC = Color.White.copy(alpha = 0.16f)
                    when (val o = shape.createOutline(size, layoutDirection, this)) {
                        is Outline.Rectangle -> drawRect(tintC)
                        is Outline.Rounded -> drawRoundRect(
                            tintC,
                            topLeft = Offset(o.roundRect.left, o.roundRect.top),
                            size = androidx.compose.ui.geometry.Size(o.roundRect.width, o.roundRect.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                o.roundRect.topLeftCornerRadius.x, o.roundRect.topLeftCornerRadius.y
                            )
                        )
                        is Outline.Generic -> drawPath(o.path, tintC)
                    }
                }
            }
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick?.let { long ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    long()
                }
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
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
            textDirection = TextDirection.Content,
            letterSpacing = spacing.sp,
            lineHeight = (size * 1.2f).sp
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

/** Six-spoke asterisk drawn by hand so it sits exactly in the middle of its key. */
@Composable
fun AsteriskIcon(modifier: Modifier = Modifier, dp: Dp = 22.dp, tint: Color = Color.White) {
    Canvas(modifier.size(dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.width / 2f
        val stroke = size.width * 0.14f
        for (deg in intArrayOf(90, 30, 150)) {
            val rad = Math.toRadians(deg.toDouble())
            val dx = (kotlin.math.cos(rad) * r).toFloat()
            val dy = (kotlin.math.sin(rad) * r).toFloat()
            drawLine(tint, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), stroke, StrokeCap.Round)
        }
    }
}
