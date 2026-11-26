package com.daljoalab.memorycapsule.core.data.repository

interface AuthRepository {

    val isSignedIn: Boolean

    val isAnonymous: Boolean

    suspend fun signInAnonymously()

    suspend fun signInWithEmail(email: String, password: String)

    suspend fun signUpWithEmail(email: String, password: String)

    fun signOut()
}
