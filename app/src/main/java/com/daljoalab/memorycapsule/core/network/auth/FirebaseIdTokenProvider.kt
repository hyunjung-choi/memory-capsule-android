package com.daljoalab.memorycapsule.core.network.auth

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseIdTokenProvider @Inject constructor(
    private val auth: FirebaseAuth,
) : IdTokenProvider {

    override fun getIdToken(forceRefresh: Boolean): String? {
        val user = auth.currentUser ?: return null
        return try {
            Tasks.await(user.getIdToken(forceRefresh), TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS).token
        } catch (e: Exception) {
            Log.w(TAG, "ID Token 발급 실패 (forceRefresh=$forceRefresh)", e)
            null
        }
    }

    private companion object {
        const val TAG = "IdTokenProvider"
        const val TOKEN_TIMEOUT_SECONDS = 10L
    }
}
