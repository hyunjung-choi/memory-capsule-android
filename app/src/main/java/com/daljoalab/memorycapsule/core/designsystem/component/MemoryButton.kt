package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daljoalab.memorycapsule.core.designsystem.preview.ComponentPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

private val ButtonShape = RoundedCornerShape(25.dp)
private val ButtonMinHeight = 74.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = ButtonShape,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ButtonMinHeight),
        shape = ButtonShape,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

@ComponentPreviews
@Composable
private fun MemoryButtonPreview() {
    MemoryCapsuleTheme {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(text = "다시 이야기하기", onClick = {})
            PrimaryButton(text = "연결하고 있어요", onClick = {}, enabled = false)
            SecondaryButton(text = "홈으로 가기", onClick = {})
        }
    }
}
