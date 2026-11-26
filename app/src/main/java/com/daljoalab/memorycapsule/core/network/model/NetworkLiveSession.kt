package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkSessionCreateRequest(
    val sdp: String,
    val client: NetworkClient,
)

@Serializable
data class NetworkClient(
    val platform: String,
    @SerialName("app_version") val appVersion: String,
)

@Serializable
data class NetworkSessionCreateResponse(
    val session: NetworkSession,
    val transport: NetworkTransport,
)

@Serializable
data class NetworkSession(
    val id: String,
    @SerialName("max_seconds") val maxSeconds: Int,
)

@Serializable
data class NetworkTransport(
    val type: String? = null,
    val sdp: String,
)
