package com.daljoalab.memorycapsule.core.model

enum class Reaction {
    NEUTRAL,
    HAPPY,
    EMPATHY,
    SURPRISED_HAPPY,
    SURPRISED_CONCERN,
}

enum class RiskLevel {
    R0,
    R1,
    R2,
    R3,
    UNKNOWN,
}

data class LiveSession(
    val id: String,
    val maxSeconds: Int,
    val answerSdp: String,
)

data class Utterance(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

data class Decision(
    val reaction: Reaction?,
    val riskLevel: RiskLevel,
    val safetyPhraseId: String?,
    val needsReview: Boolean,
    val endIntent: Boolean,
)

enum class TranscriptDirection {
    INPUT,
    OUTPUT,
}

data class TranscriptLine(
    val direction: TranscriptDirection,
    val utteranceId: String?,
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

enum class SessionEndReason {
    USER,
    LIMIT,
    ERROR,
}

data class SessionEndResult(
    val cardsCreated: Int,
    val cardsUpdated: Int,
)
