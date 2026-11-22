package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkSessionEndRequest(
    @SerialName("end_reason") val endReason: String,
    @SerialName("duration_sec") val durationSec: Int,
    val usage: NetworkVoiceUsage? = null,
    val transcript: List<NetworkTranscriptPart>,
)

@Serializable
data class NetworkVoiceUsage(
    val seconds: Double,
)

@Serializable
data class NetworkTranscriptPart(
    val direction: String,
    @SerialName("utterance_id") val utteranceId: String? = null,
    val text: String,
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
)

@Serializable
data class NetworkSessionEndResponse(
    @SerialName("cards_created") val cardsCreated: Int = 0,
    @SerialName("cards_updated") val cardsUpdated: Int = 0,
)
