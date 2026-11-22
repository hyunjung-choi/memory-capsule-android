package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkConsentState(
    val type: String,
    val version: String,
    val granted: Boolean,
    val method: String? = null,
    @SerialName("linked_senior_id") val linkedSeniorId: String? = null,
    @SerialName("granted_at") val grantedAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
