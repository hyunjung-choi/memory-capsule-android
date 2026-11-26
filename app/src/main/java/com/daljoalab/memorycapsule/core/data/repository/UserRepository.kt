package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.ConsentType
import com.daljoalab.memorycapsule.core.model.User
import com.daljoalab.memorycapsule.core.model.UserRole

interface UserRepository {

    suspend fun getMe(): User?

    suspend fun register(role: UserRole, displayName: String): User

    suspend fun grantConsents(types: List<ConsentType>, linkedSeniorId: String? = null)
}
