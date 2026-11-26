package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daljoalab.memorycapsule.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class CharacterMotion {
    CALM,
    JOYFUL,
    GENTLE,
    SURPRISED,
}

@Composable
fun MemoryCharacter(
    size: Dp,
    modifier: Modifier = Modifier,
    motion: CharacterMotion = CharacterMotion.CALM,
    speaking: Boolean = false,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val tone by animateFloatAsState(if (speaking) 1f else 0f, tween(600), label = "tone")
    val loop = rememberInfiniteTransition(label = "character")
    val phase by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(motion.periodMs, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "phase",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(motion) {
        pop.snapTo(if (motion == CharacterMotion.SURPRISED) 1.12f else 1.05f)
        pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
    }

    val wave = sin(phase * 2 * PI).toFloat()
    val clickModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .requiredSize(size * GLOW_RATIO)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) { drawGlow() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(clickModifier)
                .graphicsLayer {
                    val scale = pop.value * motion.scale(wave)
                    scaleX = scale
                    scaleY = scale * motion.squash
                    translationY = motion.lift(wave) * size.toPx()
                    rotationZ = motion.tilt(wave)
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) { drawOrb(tone) }
            Image(
                painter = painterResource(R.drawable.ic_character_face),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(FACE_WIDTH_RATIO)
                    .fillMaxHeight(FACE_HEIGHT_RATIO),
            )
        }
    }
}

private val CharacterMotion.periodMs: Int
    get() = when (this) {
        CharacterMotion.CALM -> 3600
        CharacterMotion.JOYFUL -> 900
        CharacterMotion.GENTLE -> 4200
        CharacterMotion.SURPRISED -> 1400
    }

private fun CharacterMotion.scale(wave: Float): Float = when (this) {
    CharacterMotion.CALM -> 1f + 0.02f * wave
    CharacterMotion.JOYFUL -> 1f + 0.015f * wave
    CharacterMotion.GENTLE -> 1f + 0.01f * wave
    CharacterMotion.SURPRISED -> 1.03f + 0.01f * wave
}

private val CharacterMotion.squash: Float
    get() = if (this == CharacterMotion.GENTLE) 0.97f else 1f

private fun CharacterMotion.lift(wave: Float): Float =
    if (this == CharacterMotion.JOYFUL) -0.03f * abs(wave) else 0f

private fun CharacterMotion.tilt(wave: Float): Float =
    if (this == CharacterMotion.GENTLE) 4f * wave - 3f else 0f

private const val GLOW_RATIO = 232f / 180f
private const val FACE_WIDTH_RATIO = 0.3954f
private const val FACE_HEIGHT_RATIO = 0.2727f

private fun DrawScope.drawGlow() {
    val s = size.minDimension
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0x3554AB86),
            0.57f to Color(0x0054AB86),
            center = Offset(0.32f * s, 0.35f * s),
            radius = 0.94f * s,
        ),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0x2D458F97),
            0.56f to Color(0x00458F97),
            center = Offset(0.72f * s, 0.60f * s),
            radius = 0.93f * s,
        ),
    )
    // 피그마는 blur(13px)로 가장자리를 흐린다. blur는 API 31부터라 원형 마스크로 대신 처리한다
    drawRect(
        brush = Brush.radialGradient(
            0.55f to Color.Black,
            1f to Color.Transparent,
            radius = s / 2,
        ),
        blendMode = BlendMode.DstIn,
    )
}

/** CSS 배경 순서(뒤에 적힌 것이 아래)대로 그린 뒤, 가장자리를 원형 마스크로 흐리게 자른다 */
private fun DrawScope.drawOrb(tone: Float) {
    val s = size.minDimension
    fun c(calm: Long, speaking: Long) = lerp(Color(calm), Color(speaking), tone)

    drawRect(
        brush = Brush.radialGradient(
            0.42f to c(0xFFB8DDC1, 0xFF6BAE92),
            0.62f to c(0xFF86B69D, 0xFF518F80),
            0.72f to c(0x20B7D8C0, 0x206DAA92),
            0.75f to Color.Transparent,
            center = Offset(0.5f * s, 0.5f * s),
            radius = 0.707f * s,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            0f to c(0xFF9EC5BE, 0xFF729AAB),
            0.4f to c(0xD991BDAE, 0xD94D8E99),
            0.69f to Color.Transparent,
            center = Offset(0.76f * s, 0.66f * s),
            radius = 1.0f * s,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            0f to c(0xFFE7F4E3, 0xFFB8D8C8),
            0.32f to c(0xFFC4E0CA, 0xFF7DB69B),
            0.62f to Color.Transparent,
            center = Offset(0.35f * s, 0.12f * s),
            radius = 1.08f * s,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            0f to c(0xFFBCDCC7, 0xFF62A78B),
            0.36f to c(0xF5A7D0B8, 0xF562A58E),
            0.68f to Color.Transparent,
            center = Offset(0.5f * s, 0.58f * s),
            radius = 0.76f * s,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            0.58f to Color.Black,
            0.65f to Color.Black.copy(alpha = 0.867f),
            0.73f to Color.Transparent,
            center = Offset(0.5f * s, 0.5f * s),
            radius = 0.707f * s,
        ),
        blendMode = BlendMode.DstIn,
    )
}

@Preview(name = "기본·말하는 중", showBackground = true, backgroundColor = 0xFFFBFCF9)
@Composable
private fun MemoryCharacterPreview() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            MemoryCharacter(size = 180.dp)
        }
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            MemoryCharacter(size = 232.dp, speaking = true)
        }
    }
}

@Preview(name = "몸짓", showBackground = true, backgroundColor = 0xFFFBFCF9)
@Composable
private fun MemoryCharacterMotionPreview(
    @PreviewParameter(CharacterMotionPreviewParameterProvider::class) motion: CharacterMotion,
) {
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        MemoryCharacter(size = 104.dp, motion = motion)
    }
}

private class CharacterMotionPreviewParameterProvider : PreviewParameterProvider<CharacterMotion> {
    override val values = CharacterMotion.entries.asSequence()
}
