package com.daljoalab.memorycapsule.feature.roleselect

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.SelectCard
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun RoleSelectScreen(
    onSeniorSelected: () -> Unit,
    onGuardianSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MemoryScaffold(modifier = modifier) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "누가 사용하시나요?",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        SelectCard(
            title = "시니어로 시작",
            description = "기억이와 편하게 대화해요",
            onClick = onSeniorSelected,
        )
        SelectCard(
            title = "보호자로 시작",
            description = "가족의 안부를 살펴봐요",
            onClick = onGuardianSelected,
        )
    }
}

@ScreenPreviews
@Composable
private fun RoleSelectScreenPreview() {
    MemoryCapsuleTheme {
        RoleSelectScreen(onSeniorSelected = {}, onGuardianSelected = {})
    }
}
