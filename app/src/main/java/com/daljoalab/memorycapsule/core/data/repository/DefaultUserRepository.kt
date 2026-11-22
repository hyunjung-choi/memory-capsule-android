package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.ConsentType
import com.daljoalab.memorycapsule.core.model.User
import com.daljoalab.memorycapsule.core.model.UserRole
import com.daljoalab.memorycapsule.core.network.datasource.ConsentNetworkDataSource
import com.daljoalab.memorycapsule.core.network.datasource.UserNetworkDataSource
import com.daljoalab.memorycapsule.core.network.model.NetworkConsentRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkUser
import com.daljoalab.memorycapsule.core.network.model.NetworkUserRole
import com.daljoalab.memorycapsule.core.network.model.NetworkUserUpsertRequest
import java.util.TimeZone
import javax.inject.Inject

class DefaultUserRepository @Inject constructor(
    private val userNetwork: UserNetworkDataSource,
    private val consentNetwork: ConsentNetworkDataSource,
) : UserRepository {

    override suspend fun getMe(): User? = userNetwork.getMe()?.toModel()

    override suspend fun register(role: UserRole, displayName: String): User =
        userNetwork.upsertMe(
            NetworkUserUpsertRequest(
                role = when (role) {
                    UserRole.SENIOR -> NetworkUserRole.SENIOR
                    UserRole.GUARDIAN -> NetworkUserRole.GUARDIAN
                },
                displayName = displayName.trim(),
                timezone = TimeZone.getDefault().id,
            ),
        ).toModel()

    override suspend fun grantConsents(types: List<ConsentType>, linkedSeniorId: String?) {
        types.forEach { type ->
            consentNetwork.saveConsent(
                NetworkConsentRequest(
                    type = type.serverName,
                    version = CONSENT_VERSION,
                    granted = true,
                    linkedSeniorId = linkedSeniorId.takeIf { type == ConsentType.GUARDIAN_DATA_VIEW },
                ),
            )
        }
    }

    private fun NetworkUser.toModel() = User(
        id = userId,
        role = when (role) {
            NetworkUserRole.SENIOR -> UserRole.SENIOR
            NetworkUserRole.GUARDIAN -> UserRole.GUARDIAN
        },
        displayName = displayName,
        grantedConsents = consents
            .filter { it.granted && it.revokedAt == null }
            .mapNotNull { ConsentType.fromServerName(it.type) }
            .toSet(),
    )

    private companion object {
        /** 데모용 버전. 기획에서 동의 문구·버전이 확정되면 바꾼다 */
        const val CONSENT_VERSION = "demo-2026-11"
    }
}
