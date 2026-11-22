package com.daljoalab.memorycapsule.core.network.datasource

import com.daljoalab.memorycapsule.core.network.api.PairingApi
import com.daljoalab.memorycapsule.core.network.error.ApiCaller
import com.daljoalab.memorycapsule.core.network.model.NetworkLink
import com.daljoalab.memorycapsule.core.network.model.NetworkLinkRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkPairingCode
import com.daljoalab.memorycapsule.core.network.model.NetworkPairingCodeRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkPairingTarget
import javax.inject.Inject

class PairingNetworkDataSource @Inject constructor(
    private val api: PairingApi,
    private val apiCaller: ApiCaller,
) {

    suspend fun issueCode(): NetworkPairingCode = apiCaller { api.issueCode() }

    suspend fun resolve(code: String): NetworkPairingTarget =
        apiCaller { api.resolve(NetworkPairingCodeRequest(code)) }

    suspend fun link(code: String, relation: String): NetworkLink =
        apiCaller { api.link(NetworkLinkRequest(code, relation)) }

    suspend fun getLinks(): List<NetworkLink> = apiCaller { api.getLinks() }
}
