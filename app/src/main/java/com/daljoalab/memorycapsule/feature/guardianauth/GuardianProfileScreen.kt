package com.daljoalab.memorycapsule.feature.guardianauth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.ErrorText
import com.daljoalab.memorycapsule.core.designsystem.component.LabeledTextField
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun GuardianProfileScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuardianProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.completed) {
        if (uiState.completed) onCompleted()
    }
    GuardianProfileScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onTermsCheckedChange = viewModel::onTermsCheckedChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun GuardianProfileScreen(
    uiState: GuardianProfileUiState,
    onNameChange: (String) -> Unit,
    onTermsCheckedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    MemoryScaffold(
        modifier = modifier,
        title = "보호자 정보 확인",
        onBack = onBack,
        bottomBar = {
            ErrorText(uiState.errorMessage)
            PrimaryButton(
                text = if (uiState.isSubmitting) "저장하고 있어요" else "저장하고 부모님 연결",
                onClick = onSubmit,
                enabled = uiState.canSubmit,
            )
        },
    ) {
        LabeledTextField(
            label = "이름",
            value = uiState.name,
            onValueChange = onNameChange,
            placeholder = "예: 김민수",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            contentType = ContentType.PersonFullName,
            focusRequester = focusRequester,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = uiState.termsChecked, onCheckedChange = onTermsCheckedChange)
            Text(
                text = "보호자 이용 약관과 일정 관리에 동의해요",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun GuardianProfileScreenPreview() {
    MemoryCapsuleTheme {
        GuardianProfileScreen(
            uiState = GuardianProfileUiState(name = "김민수", termsChecked = true),
            onNameChange = {},
            onTermsCheckedChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
