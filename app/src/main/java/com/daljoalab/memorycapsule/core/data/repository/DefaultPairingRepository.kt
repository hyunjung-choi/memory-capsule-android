package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.GuardianLink
import com.daljoalab.memorycapsule.core.model.PairingCode
import com.daljoalab.memorycapsule.core.model.PairingTarget
import com.daljoalab.memorycapsule.core.network.datasource.PairingNetworkDataSource
import com.daljoalab.memorycapsule.core.network.model.NetworkLink
import javax.inject.Inject

class DefaultPairingRepository @Inject constructor(
    private val network: PairingNetworkDataSource,
) : PairingRepository {

    override suspend fun issueCode(): PairingCode =
        PairingCode(code = network.issueCode().code, validForMillis = CODE_VALID_MILLIS)

    override suspend fun resolve(code: String): PairingTarget =
        network.resolve(code).let { PairingTarget(seniorId = it.seniorId, displayName = it.displayName) }

    override suspend fun link(code: String, relation: String): GuardianLink =
        network.link(code, relation).toModel()

    override suspend fun getLinks(): List<GuardianLink> = network.getLinks().map { it.toModel() }

    private fun NetworkLink.toModel() = GuardianLink(
        linkId = linkId,
        seniorId = seniorId,
        displayName = displayName ?: senior?.displayName,
        relation = relation,
    )

    private companion object {
        const val CODE_VALID_MILLIS = 5 * 60 * 1000L
    }
}
