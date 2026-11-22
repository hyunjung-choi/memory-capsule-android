package com.daljoalab.memorycapsule.feature.guardianlink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.ErrorText
import com.daljoalab.memorycapsule.core.designsystem.component.InfoCard
import com.daljoalab.memorycapsule.core.designsystem.component.LabeledTextField
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SecondaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SelectCard
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme
import com.daljoalab.memorycapsule.core.model.PairingTarget

@Composable
fun GuardianLinkScreen(
    onBack: () -> Unit,
    onLinked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuardianLinkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.linked) {
        if (uiState.linked) onLinked()
    }
    val onStepBack = { if (!viewModel.back()) onBack() }
    BackHandler(onBack = onStepBack)
    GuardianLinkScreen(
        uiState = uiState,
        onBack = onStepBack,
        onStartCodeEntry = viewModel::startCodeEntry,
        onCodeChange = viewModel::onCodeChange,
        onResolve = viewModel::resolve,
        onLink = viewModel::link,
        onRecheck = viewModel::backToCodeEntry,
        modifier = modifier,
    )
}

@Composable
private fun GuardianLinkScreen(
    uiState: GuardianLinkUiState,
    onBack: () -> Unit,
    onStartCodeEntry: () -> Unit,
    onCodeChange: (String) -> Unit,
    onResolve: () -> Unit,
    onLink: () -> Unit,
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.step) {
        GuardianLinkStep.CHOOSE_METHOD -> MemoryScaffold(modifier = modifier, title = "부모님 연결", onBack = onBack) {
            Text(
                text = "부모님 앱에서\n보호자 연결을 열어 주세요.",
                style = MaterialTheme.typography.titleMedium,
            )
            SelectCard(
                title = "인증번호 입력",
                description = "부모님 화면의 번호를 입력해요",
                onClick = onStartCodeEntry,
            )
        }

        GuardianLinkStep.ENTER_CODE -> CodeEntry(
            uiState = uiState,
            onBack = onBack,
            onCodeChange = onCodeChange,
            onResolve = onResolve,
            modifier = modifier,
        )

        GuardianLinkStep.CONFIRM -> MemoryScaffold(
            modifier = modifier,
            title = "연결할 부모님",
            onBack = onBack,
            bottomBar = {
                ErrorText(uiState.errorMessage)
                PrimaryButton(
                    text = if (uiState.isSubmitting) "연결하고 있어요" else "연결하기",
                    onClick = onLink,
                    enabled = !uiState.isSubmitting,
                )
                SecondaryButton(text = "다시 확인하기", onClick = onRecheck)
            },
        ) {
            uiState.target?.let { TargetCard(it) }
            InfoCard(
                title = "함께 살펴볼 정보",
                body = "대화 기록, 마음의 변화와\n확인이 필요한 신호",
            )
            Text(
                text = "연결하면 부모님의 정보를 볼 수 있도록 동의한 것으로 저장돼요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CodeEntry(
    uiState: GuardianLinkUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onResolve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    MemoryScaffold(
        modifier = modifier,
        title = "인증번호 입력",
        onBack = onBack,
        bottomBar = {
            ErrorText(uiState.errorMessage)
            PrimaryButton(
                text = if (uiState.isSubmitting) "확인하고 있어요" else "부모님 확인하기",
                onClick = onResolve,
                enabled = uiState.code.length == 6 && !uiState.isSubmitting,
            )
        },
    ) {
        LabeledTextField(
            label = "부모님 화면의 인증번호",
            value = uiState.code,
            onValueChange = onCodeChange,
            placeholder = "인증번호 6자리",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onResolve() }),
            focusRequester = focusRequester,
            textStyle = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "부모님 앱의 보호자 연결 화면에\n표시된 번호를 입력해 주세요.\n6자리를 다 넣으면 바로 확인해요.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TargetCard(target: PairingTarget) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("연결할 부모님", style = MaterialTheme.typography.titleMedium)
            Text(
                text = target.displayName?.let { "${it}님" } ?: "이름을 등록하지 않은 부모님",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun GuardianLinkScreenPreview(
    @PreviewParameter(GuardianLinkUiStatePreviewParameterProvider::class) uiState: GuardianLinkUiState,
) {
    MemoryCapsuleTheme {
        GuardianLinkScreen(
            uiState = uiState,
            onBack = {},
            onStartCodeEntry = {},
            onCodeChange = {},
            onResolve = {},
            onLink = {},
            onRecheck = {},
        )
    }
}

private class GuardianLinkUiStatePreviewParameterProvider : PreviewParameterProvider<GuardianLinkUiState> {
    override val values = sequenceOf(
        GuardianLinkUiState(),
        GuardianLinkUiState(step = GuardianLinkStep.ENTER_CODE, code = "4829"),
        GuardianLinkUiState(
            step = GuardianLinkStep.ENTER_CODE,
            code = "482916",
            errorMessage = "연결 코드가 올바르지 않습니다.",
        ),
        GuardianLinkUiState(
            step = GuardianLinkStep.CONFIRM,
            code = "482916",
            target = PairingTarget(seniorId = "senior", displayName = "김영희"),
        ),
    )
}
