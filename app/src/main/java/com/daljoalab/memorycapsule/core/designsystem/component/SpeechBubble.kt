package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daljoalab.memorycapsule.core.designsystem.preview.ComponentPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primaryContainer
    val style = MaterialTheme.typography.headlineLarge.let {
        if (text.length > LONG_TEXT_LENGTH) it.copy(fontSize = 28.sp, lineHeight = 40.sp) else it
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(52.dp), color = color) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                style = style,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
        Canvas(Modifier.size(width = 20.dp, height = 12.dp)) {
            val tail = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(tail, color)
        }
    }
}

@Composable
fun CaptionBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomEnd = 17.dp, bottomStart = 48.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 20.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val LONG_TEXT_LENGTH = 40

@ComponentPreviews
@Composable
private fun SpeechBubblePreview() {
    MemoryCapsuleTheme {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpeechBubble(text = "오늘 어떠셨어요?")
            SpeechBubble(text = "아, 그랬군요. 갑자기 만나서 더 반가우셨겠어요. 점심은 뭐 드셨나요?")
            CaptionBubble(text = "오늘은 조금 외로웠어요. 아침에 혼자 밥을 먹고 창밖을 한참 바라봤어요.")
        }
    }
}
