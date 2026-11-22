package com.daljoalab.memorycapsule.feature.conversation

import com.daljoalab.memorycapsule.core.model.Reaction
import com.daljoalab.memorycapsule.core.model.RiskLevel

data class ConversationUiState(
    val phase: ConversationPhase = ConversationPhase.Ready,
    val reaction: Reaction = Reaction.NEUTRAL,
    val assistantText: String = "",
    val seniorText: String = "",
    val safetyNotice: SafetyNotice? = null,
)

data class SafetyNotice(
    val phraseId: String,
    val level: RiskLevel,
    val text: String,
    val phoneNumbers: List<String>,
)

sealed interface ConversationPhase {
    data object Ready : ConversationPhase
    data object Connecting : ConversationPhase
    data object Listening : ConversationPhase
    data object Answering : ConversationPhase
    data object Closing : ConversationPhase
    data object Saving : ConversationPhase

    data class Finished(val cardsCreated: Int?) : ConversationPhase
    data class Failed(val message: String, val needsMicSettings: Boolean = false) : ConversationPhase
}

internal val ConversationPhase.isTalking: Boolean
    get() = this == ConversationPhase.Listening || this == ConversationPhase.Answering
