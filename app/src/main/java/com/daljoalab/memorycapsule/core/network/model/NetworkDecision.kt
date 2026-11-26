package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkDecisionRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("utterance_id") val utteranceId: String,
    @SerialName("input_text") val inputText: String,
    @SerialName("output_text") val outputText: String,
    @SerialName("previous_reaction") val previousReaction: String,
    val final: Boolean,
    val utterance: NetworkUtterance? = null,
)

@Serializable
data class NetworkUtterance(
    val text: String,
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
)

@Serializable
data class NetworkDecision(
    val reaction: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("assessment_status") val assessmentStatus: String? = null,
    @SerialName("safety_override") val safetyOverride: Boolean? = null,
    @SerialName("safety_phrase_id") val safetyPhraseId: String? = null,
    @SerialName("end_intent") val endIntent: String? = null,
    @SerialName("assessment_id") val assessmentId: String? = null,
)
