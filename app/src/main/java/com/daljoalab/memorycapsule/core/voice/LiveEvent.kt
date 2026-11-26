package com.daljoalab.memorycapsule.core.voice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong

sealed interface LiveEvent {

    data object Started : LiveEvent

    data class Closed(val usageSeconds: Double?, val reason: String?) : LiveEvent

    data class InputDelta(
        val eventId: String?,
        val text: String,
        val startMs: Long?,
        val endMs: Long?,
    ) : LiveEvent

    data class InputDone(val startMs: Long?, val endMs: Long?) : LiveEvent

    data class OutputDelta(
        val eventId: String?,
        val text: String,
        val startMs: Long?,
        val endMs: Long?,
    ) : LiveEvent

    data class UsageUpdated(val seconds: Double) : LiveEvent

    data class Disconnected(val cause: Cause) : LiveEvent {
        enum class Cause { CONNECTION_FAILED, CHANNEL_CLOSED }
    }
}

internal fun Json.parseLiveEvent(text: String): LiveEvent? {
    val event = runCatching { parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
    return when (event.string("type")) {
        "session.started" -> LiveEvent.Started
        "session.closed" -> LiveEvent.Closed(event.usageSeconds(), event.string("reason"))
        "session.input_transcript.delta" -> event.string("delta")
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                LiveEvent.InputDelta(event.string("event_id"), it, event.millis("start_ms"), event.millis("end_ms"))
            }
        "session.input_transcript.done", "session.input_transcript.completed" ->
            LiveEvent.InputDone(event.millis("start_ms"), event.millis("end_ms"))
        "session.output_transcript.delta" -> event.string("delta")
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                LiveEvent.OutputDelta(event.string("event_id"), it, event.millis("start_ms"), event.millis("end_ms"))
            }
        "session.usage.updated" -> event.usageSeconds()?.let(LiveEvent::UsageUpdated)
        else -> null
    }
}

private fun JsonObject.string(key: String): String? =
    (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun JsonObject.millis(key: String): Long? =
    (get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }?.roundToLong()

private fun JsonObject.usageSeconds(): Double? =
    (get("usage") as? JsonObject)?.get("seconds")?.jsonPrimitive?.doubleOrNull
