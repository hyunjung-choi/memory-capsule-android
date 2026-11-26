package com.daljoalab.memorycapsule.core.voice

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveEventTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `입력 자막 조각의 실수 시각을 밀리초로 반올림한다`() {
        val event = json.parseLiveEvent(
            """{"type":"session.input_transcript.delta","event_id":"e1","delta":"안녕","start_ms":1200.6,"end_ms":1400.4}""",
        )

        assertEquals(LiveEvent.InputDelta("e1", "안녕", 1201, 1400), event)
    }

    @Test
    fun `답변 자막 조각의 서버 시각을 읽는다`() {
        val event = json.parseLiveEvent(
            """{"type":"session.output_transcript.delta","start_ms":2400,"end_ms":2600,"delta":"안녕하세요","event_id":"e2"}""",
        )

        assertEquals(LiveEvent.OutputDelta("e2", "안녕하세요", 2400, 2600), event)
    }

    @Test
    fun `시각이 없으면 null로 둔다`() {
        val event = json.parseLiveEvent("""{"type":"session.input_transcript.done"}""")

        assertEquals(LiveEvent.InputDone(null, null), event)
    }

    @Test
    fun `빈 답변 자막 조각은 무시한다`() {
        assertNull(json.parseLiveEvent("""{"type":"session.output_transcript.delta","delta":""}"""))
    }

    @Test
    fun `종료 이벤트에서 최종 사용량과 종료 이유를 읽는다`() {
        val event = json.parseLiveEvent(
            """{"type":"session.closed","reason":"close_requested","usage":{"seconds":23}}""",
        )

        assertEquals(LiveEvent.Closed(23.0, "close_requested"), event)
    }

    @Test
    fun `모르는 이벤트와 깨진 JSON은 무시한다`() {
        assertNull(json.parseLiveEvent("""{"type":"session.something_new"}"""))
        assertNull(json.parseLiveEvent("not json"))
    }
}
