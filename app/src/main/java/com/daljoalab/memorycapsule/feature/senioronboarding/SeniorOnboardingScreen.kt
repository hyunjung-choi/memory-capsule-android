package com.daljoalab.memorycapsule.feature.senioronboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.ErrorText
import com.daljoalab.memorycapsule.core.designsystem.component.LabeledTextField
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryBird
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SecondaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SpeechBubble
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun SeniorOnboardingScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeniorOnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onCompleted()
    }
    when (uiState.step) {
        SeniorOnboardingStep.CONSENT -> ConsentStep(
            privacyChecked = uiState.privacyChecked,
            onCheckedChange = viewModel::onPrivacyCheckedChange,
            onConfirm = viewModel::confirmConsent,
            onLater = onBack,
            modifier = modifier,
        )
        SeniorOnboardingStep.NAME -> NameStep(
            uiState = uiState,
            onNameChange = viewModel::onNameChange,
            onSubmit = viewModel::submit,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ConsentStep(
    privacyChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onLater)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = DimColor) {}
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("기억캡슐", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("대화 시작 동의", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "기억이는 AI 말벗이에요.\n대화 음성과 내용은 기억 카드를 만드는 데 쓰여요. " +
                        "개인정보 처리 안내를 확인하고 대화를 시작해 주세요.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = privacyChecked, onCheckedChange = onCheckedChange)
                    Text("개인정보 처리 안내를 확인했어요", style = MaterialTheme.typography.bodyLarge)
                }
                PrimaryButton(text = "확인하고 시작", onClick = onConfirm, enabled = privacyChecked)
                SecondaryButton(text = "나중에", onClick = onLater)
            }
        }
    }
}

@Composable
private fun NameStep(
    uiState: SeniorOnboardingUiState,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    MemoryScaffold(
        modifier = modifier,
        onBack = onBack,
        bottomBar = {
            ErrorText(uiState.errorMessage)
            PrimaryButton(
                text = if (uiState.isSubmitting) "준비하고 있어요" else "입력했어",
                onClick = onSubmit,
                enabled = uiState.name.isNotBlank() && !uiState.isSubmitting,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpeechBubble(text = "어떻게\n불러드릴까요?")
            MemoryBird(size = 104.dp)
        }
        LabeledTextField(
            label = "이름",
            value = uiState.name,
            onValueChange = onNameChange,
            placeholder = "예: 김영희",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            focusRequester = focusRequester,
            textStyle = MaterialTheme.typography.titleMedium,
        )
    }
}

private val DimColor = Color(0xFFA3B1A8)

@ScreenPreviews
@Composable
private fun ConsentStepPreview() {
    MemoryCapsuleTheme {
        ConsentStep(privacyChecked = false, onCheckedChange = {}, onConfirm = {}, onLater = {})
    }
}

@ScreenPreviews
@Composable
private fun NameStepPreview() {
    MemoryCapsuleTheme {
        NameStep(uiState = SeniorOnboardingUiState(name = "김영희"), onNameChange = {}, onSubmit = {}, onBack = {})
    }
}

@ScreenPreviews
@Composable
private fun NameStepErrorPreview() {
    MemoryCapsuleTheme {
        NameStep(
            uiState = SeniorOnboardingUiState(name = "김영희", errorMessage = "인터넷 연결을 확인해 주세요."),
            onNameChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
