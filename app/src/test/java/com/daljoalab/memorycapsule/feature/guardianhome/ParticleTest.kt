package com.daljoalab.memorycapsule.feature.guardianhome

import org.junit.Assert.assertEquals
import org.junit.Test

class ParticleTest {

    @Test
    fun `받침이 있으면 으로를 붙인다`() {
        assertEquals("가족으로", "가족".withRo())
    }

    @Test
    fun `받침이 없으면 로를 붙인다`() {
        assertEquals("자녀로", "자녀".withRo())
    }

    @Test
    fun `ㄹ 받침이면 로를 붙인다`() {
        assertEquals("딸로", "딸".withRo())
    }

    @Test
    fun `한글이 아니면 로를 붙인다`() {
        assertEquals("friend로", "friend".withRo())
    }
}
