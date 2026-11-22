package com.daljoalab.memorycapsule.core.network.api

import com.daljoalab.memorycapsule.core.network.model.NetworkLink
import com.daljoalab.memorycapsule.core.network.model.NetworkLinkRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkPairingCode
import com.daljoalab.memorycapsule.core.network.model.NetworkPairingCodeRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkPairingTarget
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PairingApi {

    @POST("pairing/code")
    suspend fun issueCode(): NetworkPairingCode

    @POST("pairing/resolve")
    suspend fun resolve(@Body request: NetworkPairingCodeRequest): NetworkPairingTarget

    @POST("guardian/link")
    suspend fun link(@Body request: NetworkLinkRequest): NetworkLink

    @GET("guardian/links")
    suspend fun getLinks(): List<NetworkLink>
}
