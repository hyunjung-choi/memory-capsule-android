package com.daljoalab.memorycapsule.feature.guardianauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianAuthUiStateTest {

    @Test
    fun `아무것도 입력하지 않았으면 오류를 보여주지 않는다`() {
        val state = GuardianAuthUiState()

        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.passwordConfirmError)
    }

    @Test
    fun `이메일 형식이 아니면 오류를 보여주고 로그인할 수 없다`() {
        val state = GuardianAuthUiState(email = "guardian@example", password = "secret")

        assertEquals("이메일 형식을 확인해 주세요.", state.emailError)
        assertFalse(state.canSignIn)
    }

    @Test
    fun `비밀번호가 6자보다 짧으면 가입할 수 없다`() {
        val state = GuardianAuthUiState(email = "guardian@example.com", password = "12345", passwordConfirm = "12345")

        assertEquals("6자 이상 입력해 주세요.", state.passwordError)
        assertFalse(state.canSignUp)
    }

    @Test
    fun `비밀번호 확인이 다르면 가입할 수 없다`() {
        val state = GuardianAuthUiState(email = "guardian@example.com", password = "secret", passwordConfirm = "secre")

        assertEquals("비밀번호가 서로 달라요.", state.passwordConfirmError)
        assertFalse(state.canSignUp)
    }

    @Test
    fun `모두 올바르면 가입할 수 있다`() {
        val state = GuardianAuthUiState(email = "guardian@example.com", password = "secret", passwordConfirm = "secret")

        assertTrue(state.canSignUp)
        assertTrue(state.canSignIn)
    }

    @Test
    fun `제출 중에는 다시 누를 수 없다`() {
        val state = GuardianAuthUiState(
            email = "guardian@example.com",
            password = "secret",
            passwordConfirm = "secret",
            isSubmitting = true,
        )

        assertFalse(state.canSignIn)
        assertFalse(state.canSignUp)
    }
}
