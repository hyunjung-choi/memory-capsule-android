package com.daljoalab.memorycapsule.feature.seniorpairing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.InfoCard
import com.daljoalab.memorycapsule.core.designsystem.component.KeepScreenOn
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryCharacter
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SecondaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SpeechBubble
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun SeniorPairingScreen(
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeniorPairingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    KeepScreenOn(enabled = (uiState as? SeniorPairingUiState.ShowingCode)?.remainingSeconds?.let { it > 0 } == true)
    SeniorPairingScreen(
        uiState = uiState,
        onGrantConsent = viewModel::grantShareConsent,
        onAddAnother = viewModel::addAnotherGuardian,
        onReissue = viewModel::reissueCode,
        onRetry = viewModel::load,
        onNavigateHome = onNavigateHome,
        modifier = modifier,
    )
}

@Composable
private fun SeniorPairingScreen(
    uiState: SeniorPairingUiState,
    onGrantConsent: () -> Unit,
    onAddAnother: () -> Unit,
    onReissue: () -> Unit,
    onRetry: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MemoryScaffold(
        modifier = modifier,
        onBack = onNavigateHome,
        bottomBar = {
            when (uiState) {
                SeniorPairingUiState.NeedsConsent -> {
                    PrimaryButton(text = "동의하고 번호 받기", onClick = onGrantConsent)
                    SecondaryButton(text = "홈으로 가기", onClick = onNavigateHome)
                }
                is SeniorPairingUiState.ShowingCode -> {
                    if (uiState.remainingSeconds == 0) PrimaryButton(text = "새 번호 받기", onClick = onReissue)
                    SecondaryButton(text = "홈으로 가기", onClick = onNavigateHome)
                }
                is SeniorPairingUiState.AlreadyLinked -> {
                    PrimaryButton(text = "기억이와 이야기하기", onClick = onNavigateHome)
                    SecondaryButton(text = "다른 보호자 연결하기", onClick = onAddAnother)
                }
                is SeniorPairingUiState.Linked -> PrimaryButton(text = "기억이와 이야기하기", onClick = onNavigateHome)
                is SeniorPairingUiState.Failed -> {
                    PrimaryButton(text = "다시 해 보기", onClick = onRetry)
                    SecondaryButton(text = "홈으로 가기", onClick = onNavigateHome)
                }
                SeniorPairingUiState.Loading -> Unit
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpeechBubble(text = uiState.bubbleText())
            MemoryCharacter(size = 104.dp)
        }
        when (uiState) {
            SeniorPairingUiState.NeedsConsent -> InfoCard(
                title = "보호자와 함께 살펴봐요",
                body = "연결한 보호자는 대화 속 마음 상태와 확인이 필요한 신호, 일정을 볼 수 있어요.",
            )
            is SeniorPairingUiState.ShowingCode -> CodeCard(uiState)
            is SeniorPairingUiState.AlreadyLinked -> InfoCard(
                title = "연결된 보호자",
                body = uiState.guardianNames.joinToString("\n") { name -> name?.let { "${it}님" } ?: "이름 없는 보호자" },
            )
            else -> Unit
        }
    }
}

@Composable
private fun CodeCard(state: SeniorPairingUiState.ShowingCode) {
    val expired = state.remainingSeconds == 0
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("보호자 연결 인증번호", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.code.chunked(3).joinToString(" "),
                modifier = Modifier.semantics { contentDescription = state.code.toList().joinToString(" ") },
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = if (expired) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (expired) {
                    "시간이 지났어요"
                } else {
                    "%d분 %02d초 남았어요".format(state.remainingSeconds / 60, state.remainingSeconds % 60)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

private fun SeniorPairingUiState.bubbleText(): String = when (this) {
    SeniorPairingUiState.Loading -> "잠시만 기다려 주세요"
    SeniorPairingUiState.NeedsConsent -> "보호자와 연결하려면\n동의가 필요해요."
    is SeniorPairingUiState.AlreadyLinked -> "이미 보호자와\n연결돼 있어요."
    is SeniorPairingUiState.ShowingCode ->
        if (remainingSeconds == 0) "새 번호를\n보여드릴게요." else "보호자 휴대폰에\n이 번호를 입력해\n주세요."
    is SeniorPairingUiState.Linked ->
        "${guardianName?.let { "${it}님과 " }.orEmpty()}연결됐어요.\n이제 가족과 함께\n안부를 살펴볼 수 있어요."
    is SeniorPairingUiState.Failed -> message
}

@ScreenPreviews
@Composable
private fun SeniorPairingScreenPreview(
    @PreviewParameter(SeniorPairingUiStatePreviewParameterProvider::class) uiState: SeniorPairingUiState,
) {
    MemoryCapsuleTheme {
        SeniorPairingScreen(
            uiState = uiState,
            onGrantConsent = {},
            onAddAnother = {},
            onReissue = {},
            onRetry = {},
            onNavigateHome = {},
        )
    }
}

private class SeniorPairingUiStatePreviewParameterProvider : PreviewParameterProvider<SeniorPairingUiState> {
    override val values = sequenceOf(
        SeniorPairingUiState.Loading,
        SeniorPairingUiState.NeedsConsent,
        SeniorPairingUiState.ShowingCode(code = "482916", remainingSeconds = 272),
        SeniorPairingUiState.ShowingCode(code = "482916", remainingSeconds = 0),
        SeniorPairingUiState.AlreadyLinked(guardianNames = listOf("김민수", null)),
        SeniorPairingUiState.Linked(guardianName = "김민수"),
        SeniorPairingUiState.Failed("지금은 연결이 어려워요. 잠시 후 다시 해 주세요."),
    )
}
