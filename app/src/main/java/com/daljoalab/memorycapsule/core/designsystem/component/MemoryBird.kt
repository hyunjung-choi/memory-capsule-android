package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.daljoalab.memorycapsule.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

enum class BirdExpression(val label: String) {
    NEUTRAL("기본"),
    HAPPY("기쁨"),
    EMPATHY("공감"),
    SURPRISED("놀람"),
}

@Composable
fun MemoryBird(
    size: Dp,
    modifier: Modifier = Modifier,
    expression: BirdExpression = BirdExpression.NEUTRAL,
    intensity: Float = 1f,
    speaking: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "${expression.label} 표정의 기억이",
) {
    val body = ImageBitmap.imageResource(R.drawable.bird_body)
    val wing = ImageBitmap.imageResource(R.drawable.bird_wing)
    val animator = remember { BirdAnimator() }
    var frame by remember { mutableStateOf(BirdFrame.Rest) }

    LaunchedEffect(expression, intensity) {
        withFrameNanos { now -> animator.setExpression(expression, intensity, now) }
    }
    LaunchedEffect(speaking) {
        while (true) {
            withFrameNanos { now -> frame = animator.frame(now, speaking) }
        }
    }

    val clickModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .then(clickModifier)
            .graphicsLayer {
                scaleX = frame.body.x
                scaleY = frame.body.y + frame.breath
                transformOrigin = TransformOrigin(0.5f, 0.85f)
                translationY = -frame.body.lift * 0.85f * this.size.height
            },
    ) {
        val s = this.size.minDimension / CANVAS
        scale(s, s, pivot = Offset.Zero) {
            translate(50f, 20f) {
                drawImage(body, dstOffset = IntOffset.Zero, dstSize = IntSize(1000, 1000), filterQuality = FilterQuality.High)
                drawWing(wing, side = -1f, frame)
                drawWing(wing, side = 1f, frame)
                translate(0f, 101f) { drawFeatures(frame) }
            }
        }
    }
}

private const val CANVAS = 1100f

private data class BirdPose(val eye: Float, val smile: Float, val brow: Float, val open: Float, val blush: Float) {
    companion object {
        val Neutral = BirdPose(eye = 1f, smile = 0f, brow = 0f, open = 0f, blush = 0.12f)
        private val Happy = BirdPose(eye = 0.04f, smile = 1f, brow = 0f, open = 0.12f, blush = 0.38f)
        private val Empathy = BirdPose(eye = 0.82f, smile = 0f, brow = 1f, open = 0f, blush = 0.15f)
        private val Surprised = BirdPose(eye = 1.22f, smile = 0f, brow = -0.65f, open = 0.72f, blush = 0.12f)

        fun of(e: BirdExpression) = when (e) {
            BirdExpression.NEUTRAL -> Neutral
            BirdExpression.HAPPY -> Happy
            BirdExpression.EMPATHY -> Empathy
            BirdExpression.SURPRISED -> Surprised
        }

        fun mix(a: BirdPose, b: BirdPose, t: Float): BirdPose {
            fun m(x: Float, y: Float) = x + (y - x) * t
            return BirdPose(m(a.eye, b.eye), m(a.smile, b.smile), m(a.brow, b.brow), m(a.open, b.open), m(a.blush, b.blush))
        }
    }
}

private data class BodyMotion(val x: Float = 1f, val y: Float = 1f, val lift: Float = 0f) {
    companion object {
        val Rest = BodyMotion()
    }
}

private data class BirdFrame(
    val pose: BirdPose,
    val blink: Float,
    val mouth: Float,
    val wingAngle: Float,
    val wingLength: Float,
    val body: BodyMotion,
    val breath: Float,
) {
    companion object {
        val Rest = BirdFrame(BirdPose.Neutral, 0f, 0f, 0f, 1f, BodyMotion.Rest, 0f)
    }
}

private object BirdMotion {
    // [초, 가로 배율, 세로 배율, 점프 높이]
    private val frames = mapOf(
        BirdExpression.HAPPY to listOf(
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0.12f, 1.1f, 0.9f, 0f), floatArrayOf(0.23f, 0.94f, 1.13f, 0.035f),
            floatArrayOf(0.4f, 0.98f, 1.05f, 0.12f), floatArrayOf(0.58f, 1f, 1f, 0.045f),
            floatArrayOf(0.68f, 1.12f, 0.87f, 0f), floatArrayOf(0.82f, 1f, 1f, 0f),
        ),
        BirdExpression.SURPRISED to listOf(
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0.1f, 1.06f, 0.94f, 0f), floatArrayOf(0.27f, 0.9f, 1.2f, 0.035f),
            floatArrayOf(0.5f, 0.96f, 1.08f, 0.015f), floatArrayOf(0.72f, 1.04f, 0.96f, 0f), floatArrayOf(1f, 1f, 1f, 0f),
        ),
        BirdExpression.EMPATHY to listOf(
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0.42f, 1.045f, 0.94f, 0f),
            floatArrayOf(0.9f, 0.98f, 1.025f, 0f), floatArrayOf(1.65f, 1f, 1f, 0f),
        ),
    )

    fun smooth(t: Float) = t * t * (3 - 2 * t)

    fun body(e: BirdExpression, elapsed: Float): BodyMotion {
        val f = frames[e] ?: return BodyMotion.Rest
        val duration = f.last()[0]
        val happy = e == BirdExpression.HAPPY
        val hop = if (happy) (elapsed / duration).toInt() else 0
        if (hop >= 3 || (!happy && elapsed >= duration)) return BodyMotion.Rest
        val time = if (happy) elapsed % duration else elapsed
        val end = f.indexOfFirst { it[0] >= time }
        if (end <= 0) return BodyMotion.Rest
        val a = f[end - 1]
        val b = f[end]
        val ease = smooth((time - a[0]) / (b[0] - a[0]))
        val strength = if (happy) 1 - hop * 0.15f else 1f
        fun mix(x: Float, y: Float) = x + (y - x) * ease
        return BodyMotion(
            x = 1 + (mix(a[1], b[1]) - 1) * strength,
            y = 1 + (mix(a[2], b[2]) - 1) * strength,
            lift = mix(a[3], b[3]) * strength,
        )
    }

    fun wing(e: BirdExpression, elapsed: Float): Pair<Float, Float> {
        if (e == BirdExpression.EMPATHY) return (-PI / 2).toFloat() to 1.35f
        if (e == BirdExpression.NEUTRAL) return 0f to 1f
        val flaps = if (e == BirdExpression.HAPPY) 2f else 1f
        val duration = if (e == BirdExpression.HAPPY) 0.7f else 0.9f
        val time = elapsed - 0.16f
        if (time <= 0 || time >= flaps * duration) return 0f to 1f
        return sin(PI * time / duration).toFloat().pow(2) * 2.25f to 1f
    }
}

private class BirdAnimator {
    private var expression = BirdExpression.NEUTRAL
    private var from = BirdPose.Neutral
    private var target = BirdPose.Neutral
    private var current = BirdPose.Neutral
    private var changedAt = Long.MIN_VALUE / 2
    private var motionFrom = BodyMotion.Rest
    private var motion = BodyMotion.Rest
    private var wingAngle = 0f
    private var wingLength = 1f
    private var mouth = 0f
    private var lastTick = 0L

    fun setExpression(e: BirdExpression, intensity: Float, now: Long) {
        expression = e
        from = current
        target = BirdPose.mix(BirdPose.Neutral, BirdPose.of(e), intensity)
        motionFrom = motion
        changedAt = now
    }

    fun frame(now: Long, speaking: Boolean): BirdFrame {
        val dt = if (lastTick == 0L) 0.016f else ((now - lastTick) / 1e9f).coerceIn(0.001f, 0.05f)
        lastTick = now
        val elapsed = ((now - changedAt) / 1e9).coerceAtMost(1e6).toFloat()
        current = BirdPose.mix(from, target, BirdMotion.smooth(min(1f, elapsed / 0.52f)))

        val next = BirdMotion.body(expression, elapsed)
        val blend = min(1f, elapsed / 0.14f)
        motion = BodyMotion(
            x = motionFrom.x + (next.x - motionFrom.x) * blend,
            y = motionFrom.y + (next.y - motionFrom.y) * blend,
            lift = motionFrom.lift + (next.lift - motionFrom.lift) * blend,
        )

        val (angle, length) = BirdMotion.wing(expression, elapsed)
        val k = 1 - exp(-dt / 0.08f)
        wingAngle += (angle - wingAngle) * k
        wingLength += (length - wingLength) * k

        val ms = now / 1_000_000.0
        val mouthLevel = if (speaking) max(0.0, sin(ms / 95)).toFloat() * 0.6f else 0f
        mouth += (mouthLevel - mouth) * (1 - exp(-dt / 0.065f))
        val blinkPhase = (ms % 4700 / 180).toFloat()
        val blink = if (blinkPhase < 1) sin(PI * blinkPhase).toFloat().pow(2) else 0f
        val breath = sin(ms / 1000 * 1.7).toFloat() * 0.006f
        return BirdFrame(current, blink, mouth, wingAngle, wingLength, motion, breath)
    }
}

private val WingAxis = atan2(184.0, -106.0).toFloat()

private fun DrawScope.drawWing(wing: ImageBitmap, side: Float, f: BirdFrame) {
    withTransform({
        translate(if (side < 0) 250f else 750f, 560f)
        if (side > 0) scale(-1f, 1f, pivot = Offset.Zero)
        rotate(f.wingAngle.toDegrees(), pivot = Offset.Zero)
        rotate(WingAxis.toDegrees(), pivot = Offset.Zero)
        scale(f.wingLength, 1f, pivot = Offset.Zero)
        rotate(-WingAxis.toDegrees(), pivot = Offset.Zero)
    }) {
        drawImage(wing, dstOffset = IntOffset(-146, -36), dstSize = IntSize(190, 254), filterQuality = FilterQuality.High)
    }
}

private fun Float.toDegrees() = this * 180f / PI.toFloat()

private fun hex(rgb: Long, alpha: Float = 1f) = Color(0xFF000000 or rgb).copy(alpha = alpha.coerceIn(0f, 1f))

private fun DrawScope.drawFeatures(f: BirdFrame) {
    val p = f.pose
    val cy = 359f
    for (side in floatArrayOf(-1f, 1f)) {
        val x = 500 + side * 106
        drawRect(
            brush = Brush.radialGradient(
                listOf(hex(0xDA8B56, p.blush), hex(0xDA8B56, 0f)),
                center = Offset(x + side * 27, cy + 71),
                radius = 51f,
            ),
            topLeft = Offset(x - 65, cy + 20),
            size = Size(130f, 105f),
        )

        val openness = max(0.025f, p.eye * (1 - f.blink))
        val eyeH = 42 * openness
        translate(x, cy) {
            if (openness > 0.09f) {
                drawOval(
                    brush = Brush.linearGradient(
                        0f to hex(0x66533F), 0.42f to hex(0x342B22), 1f to hex(0x241F1A),
                        start = Offset(-25f, -eyeH),
                        end = Offset(20f, eyeH),
                    ),
                    topLeft = Offset(-29f, -eyeH),
                    size = Size(58f, eyeH * 2),
                )
                drawOval(hex(0x1F1C19), topLeft = Offset(1f - 22f, 3 - eyeH * 0.82f), size = Size(44f, eyeH * 1.64f))
                val glossAlpha = min(1f, openness * 1.5f) * (1 - p.smile * 0.8f)
                val gh = min(10f, eyeH * 0.24f)
                drawOval(hex(0xFFFAF0), topLeft = Offset(-16f, -eyeH * 0.38f - gh), size = Size(16f, gh * 2), alpha = glossAlpha)
                drawOval(hex(0xFFFAF0), topLeft = Offset(6.5f, eyeH * 0.43f - 4), size = Size(7f, 8f), alpha = glossAlpha * 0.45f)
            }
            if (p.smile > 0.05f || openness <= 0.09f) {
                val smile = Path().apply {
                    moveTo(-27f, 3f)
                    quadraticTo(0f, -34 * p.smile, 27f, 3f)
                }
                drawPath(
                    smile,
                    hex(0x403A2B),
                    alpha = if (openness <= 0.09f) 1f else p.smile.coerceIn(0f, 1f),
                    style = Stroke(width = if (p.smile > 0.05f) 18f else 8f, cap = StrokeCap.Round),
                )
            }
        }

        val brow = Path().apply {
            moveTo(x - 25, cy - 66 - side * p.brow * 12)
            quadraticTo(x, cy - 73, x + 25, cy - 66 + side * p.brow * 12)
        }
        drawPath(brow, hex(0x726E4E), alpha = min(0.75f, abs(p.brow) * 0.8f), style = Stroke(width = 7f, cap = StrokeCap.Round))
    }

    val open = min(1f, p.open + f.mouth * 0.8f)
    translate(500f, 430f) {
        val mouthH = 6 + open * 29
        drawOval(hex(0x664327), topLeft = Offset(-31f, 14 + open * 16 - mouthH), size = Size(62f, mouthH * 2))
        if (open > 0.2f) {
            drawOval(hex(0xCF9870), topLeft = Offset(-16f, 24 + open * 22 - 6), size = Size(32f, 12f))
        }
        val upper = Path().apply {
            moveTo(-35f, 6f)
            quadraticTo(-24f, -26f, 0f, -25f)
            quadraticTo(24f, -26f, 35f, 6f)
            quadraticTo(0f, 24f, -35f, 6f)
        }
        drawPath(
            upper,
            Brush.linearGradient(
                0f to hex(0xF2CF8E), 0.5f to hex(0xDBA856), 1f to hex(0xAF7939),
                start = Offset(-30f, -26f),
                end = Offset(20f, 20f),
            ),
        )
        val lower = Path().apply {
            moveTo(-29f, 15 + open * 35)
            quadraticTo(0f, 37 + open * 34, 29f, 15 + open * 35)
            quadraticTo(0f, 25 + open * 35, -29f, 15 + open * 35)
        }
        drawPath(
            lower,
            Brush.linearGradient(
                listOf(hex(0xEDC37A), hex(0xB68642)),
                start = Offset(0f, 15f),
                end = Offset(0f, 40 + open * 25),
            ),
        )
        for (nx in floatArrayOf(-12f, 12f)) {
            drawOval(hex(0x9C763E), topLeft = Offset(nx - 2.5f, -8.7f), size = Size(5f, 3.4f), alpha = 0.55f)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBFCF9, widthDp = 720)
@Composable
private fun MemoryBirdPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BirdExpression.entries.forEach { MemoryBird(size = 172.dp, expression = it) }
    }
}
