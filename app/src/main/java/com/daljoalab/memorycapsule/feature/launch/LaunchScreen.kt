package com.daljoalab.memorycapsule.feature.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryCharacter
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SpeechBubble
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun LaunchScreen(
    onDecided: (LaunchDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LaunchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val decided = uiState as? LaunchUiState.Decided
    LaunchedEffect(decided) {
        decided?.let { onDecided(it.destination) }
    }
    LaunchScreen(uiState = uiState, onRetry = viewModel::decide, modifier = modifier)
}

@Composable
private fun LaunchScreen(
    uiState: LaunchUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            val failed = uiState as? LaunchUiState.Failed
            SpeechBubble(text = failed?.message ?: "잠시만 기다려 주세요")
            MemoryCharacter(size = 180.dp)
            if (failed != null) {
                PrimaryButton(text = "다시 해 보기", onClick = onRetry)
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun LaunchScreenPreview(
    @PreviewParameter(LaunchUiStatePreviewParameterProvider::class) uiState: LaunchUiState,
) {
    MemoryCapsuleTheme {
        LaunchScreen(uiState = uiState, onRetry = {})
    }
}

private class LaunchUiStatePreviewParameterProvider : PreviewParameterProvider<LaunchUiState> {
    override val values = sequenceOf(
        LaunchUiState.Loading,
        LaunchUiState.Failed("인터넷 연결을 확인해 주세요."),
    )
}
