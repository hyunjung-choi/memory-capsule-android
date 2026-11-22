package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkConsentRequest(
    val type: String,
    val version: String,
    val granted: Boolean,
    val method: String = "screen_tap",
    @SerialName("linked_senior_id") val linkedSeniorId: String? = null,
)
