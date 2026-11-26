package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daljoalab.memorycapsule.core.designsystem.preview.ComponentPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) {
        Text(
            text = message,
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@ComponentPreviews
@Composable
private fun ErrorTextPreview() {
    MemoryCapsuleTheme {
        ErrorText(message = "이메일이나 비밀번호를 다시 확인해 주세요.", modifier = Modifier.padding(22.dp))
    }
}
