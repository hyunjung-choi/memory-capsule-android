package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.GuardianLink
import com.daljoalab.memorycapsule.core.model.PairingCode
import com.daljoalab.memorycapsule.core.model.PairingTarget

interface PairingRepository {

    suspend fun issueCode(): PairingCode

    suspend fun resolve(code: String): PairingTarget

    suspend fun link(code: String, relation: String): GuardianLink

    suspend fun getLinks(): List<GuardianLink>
}
