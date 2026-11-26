package com.daljoalab.memorycapsule.core.data.repository

import com.daljoalab.memorycapsule.core.model.Decision
import com.daljoalab.memorycapsule.core.model.LiveSession
import com.daljoalab.memorycapsule.core.model.Reaction
import com.daljoalab.memorycapsule.core.model.SessionEndReason
import com.daljoalab.memorycapsule.core.model.SessionEndResult
import com.daljoalab.memorycapsule.core.model.TranscriptLine
import com.daljoalab.memorycapsule.core.model.Utterance

interface ConversationRepository {

    suspend fun createSession(offerSdp: String, appVersion: String): LiveSession

    suspend fun decideFinal(
        sessionId: String,
        utteranceId: String,
        inputText: String,
        outputText: String,
        previousReaction: Reaction,
        utterance: Utterance,
    ): Decision

    suspend fun endSession(
        sessionId: String,
        reason: SessionEndReason,
        durationSec: Int,
        usageSeconds: Double?,
        transcript: List<TranscriptLine>,
    ): SessionEndResult
}
