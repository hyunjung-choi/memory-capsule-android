package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.UserFacingException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    private val anonymousSignInMutex = Mutex()

    override val isSignedIn: Boolean
        get() = auth.currentUser != null

    override val isAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous == true

    override suspend fun signInAnonymously() = anonymousSignInMutex.withLock {
        if (auth.currentUser == null) auth.signInAnonymously().awaitAuth()
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).awaitAuth()
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).awaitAuth()
    }

    override fun signOut() = auth.signOut()

    private suspend fun <T> Task<T>.awaitAuth() {
        try {
            await()
        } catch (e: Exception) {
            throw UserFacingException(e.toAuthMessage(), e)
        }
    }

    private fun Exception.toAuthMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상으로 정해 주세요."
        is FirebaseAuthUserCollisionException -> "이미 가입된 이메일이에요. 로그인해 주세요."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException,
        -> "이메일이나 비밀번호를 다시 확인해 주세요."
        is FirebaseNetworkException -> "인터넷 연결을 확인해 주세요."
        else -> "로그인하지 못했어요. 잠시 후 다시 해 주세요."
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val exception = task.exception
        if (exception == null) continuation.resume(task.result) else continuation.resumeWithException(exception)
    }
}
