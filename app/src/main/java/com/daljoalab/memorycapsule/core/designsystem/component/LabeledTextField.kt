package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daljoalab.memorycapsule.core.designsystem.preview.ComponentPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentType: ContentType? = null,
    focusRequester: FocusRequester? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(if (contentType != null) Modifier.semantics { this.contentType = contentType } else Modifier),
            placeholder = placeholder?.let { { Text(it, style = textStyle) } },
            trailingIcon = if (isPassword) {
                {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "숨기기" else "보기", fontSize = 16.sp)
                    }
                }
            } else {
                null
            },
            supportingText = errorMessage?.let { { Text(it, fontSize = 16.sp) } },
            isError = errorMessage != null,
            singleLine = true,
            textStyle = textStyle,
            shape = RoundedCornerShape(14.dp),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }
}

object CredentialKeyboardOptions {

    private val English = LocaleList("en")

    fun email(imeAction: ImeAction = ImeAction.Next) = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = imeAction,
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        hintLocales = English,
    )

    fun password(imeAction: ImeAction) = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        hintLocales = English,
    )
}

@ComponentPreviews
@Composable
private fun LabeledTextFieldPreview() {
    MemoryCapsuleTheme {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LabeledTextField(label = "이메일", value = "", onValueChange = {}, placeholder = "이메일 주소")
            LabeledTextField(label = "비밀번호", value = "secret", onValueChange = {}, isPassword = true)
            LabeledTextField(
                label = "이메일",
                value = "guardian@",
                onValueChange = {},
                errorMessage = "이메일 형식을 확인해 주세요.",
            )
            LabeledTextField(
                label = "부모님 화면의 인증번호",
                value = "482916",
                onValueChange = {},
                textStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
