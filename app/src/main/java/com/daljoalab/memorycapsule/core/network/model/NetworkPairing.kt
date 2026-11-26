package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkPairingCode(
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("qr_payload") val qrPayload: String? = null,
)

@Serializable
data class NetworkPairingCodeRequest(
    val code: String,
)

@Serializable
data class NetworkPairingTarget(
    @SerialName("senior_id") val seniorId: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class NetworkLinkRequest(
    val code: String,
    val relation: String,
)

@Serializable
data class NetworkLink(
    @SerialName("link_id") val linkId: String,
    @SerialName("senior_id") val seniorId: String,
    @SerialName("guardian_id") val guardianId: String,
    @SerialName("display_name") val displayName: String? = null,
    val relation: String? = null,
    @SerialName("linked_at") val linkedAt: String? = null,
    val senior: NetworkLinkedSenior? = null,
)

@Serializable
data class NetworkLinkedSenior(
    @SerialName("display_name") val displayName: String? = null,
)
