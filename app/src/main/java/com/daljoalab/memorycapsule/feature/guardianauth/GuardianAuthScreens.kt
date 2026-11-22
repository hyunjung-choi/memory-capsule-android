package com.daljoalab.memorycapsule.feature.guardianauth

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.CredentialKeyboardOptions
import com.daljoalab.memorycapsule.core.designsystem.component.ErrorText
import com.daljoalab.memorycapsule.core.designsystem.component.InfoCard
import com.daljoalab.memorycapsule.core.designsystem.component.LabeledTextField
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SecondaryButton
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun GuardianLoginScreen(
    onBack: () -> Unit,
    onSignUp: () -> Unit,
    onResult: (GuardianAuthResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuardianAuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.result) { uiState.result?.let(onResult) }
    GuardianLoginScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignIn = viewModel::signIn,
        onSignUp = onSignUp,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun GuardianLoginScreen(
    uiState: GuardianAuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MemoryScaffold(
        modifier = modifier,
        title = "보호자 로그인",
        onBack = onBack,
        bottomBar = {
            ErrorText(uiState.errorMessage)
            PrimaryButton(
                text = if (uiState.isSubmitting) "확인하고 있어요" else "로그인",
                onClick = onSignIn,
                enabled = uiState.canSignIn,
            )
            SecondaryButton(text = "회원가입", onClick = onSignUp)
        },
    ) {
        EmailField(value = uiState.email, onValueChange = onEmailChange, errorMessage = uiState.emailError)
        PasswordField(
            label = "비밀번호",
            value = uiState.password,
            onValueChange = onPasswordChange,
            contentType = ContentType.Password,
            imeAction = ImeAction.Done,
            onDone = onSignIn,
        )
        InfoCard(
            title = "가족의 안부를 살펴봐요",
            body = "마음 상태·대화 기록·감정 변화와\n확인이 필요한 신호를 봐요.",
        )
    }
}

@Composable
fun GuardianSignUpScreen(
    onBack: () -> Unit,
    onResult: (GuardianAuthResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuardianAuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.result) { uiState.result?.let(onResult) }
    GuardianSignUpScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
        onSignUp = viewModel::signUp,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun GuardianSignUpScreen(
    uiState: GuardianAuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onSignUp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MemoryScaffold(
        modifier = modifier,
        title = "보호자 회원가입",
        onBack = onBack,
        bottomBar = {
            ErrorText(uiState.errorMessage)
            PrimaryButton(
                text = if (uiState.isSubmitting) "가입하고 있어요" else "다음",
                onClick = onSignUp,
                enabled = uiState.canSignUp,
            )
        },
    ) {
        EmailField(value = uiState.email, onValueChange = onEmailChange, errorMessage = uiState.emailError)
        PasswordField(
            label = "비밀번호",
            value = uiState.password,
            onValueChange = onPasswordChange,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Next,
            errorMessage = uiState.passwordError,
        )
        PasswordField(
            label = "비밀번호 확인",
            value = uiState.passwordConfirm,
            onValueChange = onPasswordConfirmChange,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Done,
            errorMessage = uiState.passwordConfirmError,
            onDone = onSignUp,
        )
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit, errorMessage: String?) {
    LabeledTextField(
        label = "이메일",
        value = value,
        onValueChange = onValueChange,
        placeholder = "이메일 주소",
        errorMessage = errorMessage,
        keyboardOptions = CredentialKeyboardOptions.email(),
        contentType = ContentType.EmailAddress + ContentType.Username,
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    contentType: ContentType,
    imeAction: ImeAction,
    errorMessage: String? = null,
    onDone: (() -> Unit)? = null,
) {
    LabeledTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = "6자 이상",
        isPassword = true,
        errorMessage = errorMessage,
        keyboardOptions = CredentialKeyboardOptions.password(imeAction),
        keyboardActions = KeyboardActions(onDone = onDone?.let { { it() } }),
        contentType = contentType,
    )
}

@ScreenPreviews
@Composable
private fun GuardianLoginScreenPreview() {
    MemoryCapsuleTheme {
        GuardianLoginScreen(
            uiState = GuardianAuthUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onSignUp = {},
            onBack = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun GuardianLoginScreenErrorPreview() {
    MemoryCapsuleTheme {
        GuardianLoginScreen(
            uiState = GuardianAuthUiState(
                email = "guardian@example.com",
                password = "secret",
                errorMessage = "이메일이나 비밀번호를 다시 확인해 주세요.",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onSignUp = {},
            onBack = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun GuardianSignUpScreenPreview() {
    MemoryCapsuleTheme {
        GuardianSignUpScreen(
            uiState = GuardianAuthUiState(
                email = "guardian@example.com",
                password = "secret",
                passwordConfirm = "secre",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onSignUp = {},
            onBack = {},
        )
    }
}
