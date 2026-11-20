package com.daljoalab.memorycapsule.core.network.auth

import com.daljoalab.memorycapsule.core.network.NetworkFactory
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: FakeIdTokenProvider

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenProvider = FakeIdTokenProvider()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `로그인 상태면 캐시된 토큰을 Authorization 헤더에 붙인다`() {
        server.enqueue(ok())

        call()

        assertEquals("Bearer $CACHED", server.takeRequest().headers["Authorization"])
        assertEquals(listOf(false), tokenProvider.calls)
    }

    @Test
    fun `로그인 전이면 Authorization 헤더 없이 보낸다`() {
        tokenProvider.loggedIn = false
        server.enqueue(ok())

        call()

        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `401이면 토큰을 강제 갱신해서 다시 보낸다`() {
        server.enqueue(unauthorized())
        server.enqueue(ok())

        val code = call()

        assertEquals(200, code)
        assertEquals(2, server.requestCount)
        assertEquals("Bearer $CACHED", server.takeRequest().headers["Authorization"])
        assertEquals("Bearer $FRESH", server.takeRequest().headers["Authorization"])
        assertEquals(listOf(false, true), tokenProvider.calls)
    }

    @Test
    fun `갱신한 토큰으로도 401이면 더 재시도하지 않는다`() {
        server.enqueue(unauthorized())
        server.enqueue(unauthorized())
        server.enqueue(ok())

        val code = call()

        assertEquals(401, code)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `로그인 전 401이면 재시도하지 않는다`() {
        tokenProvider.loggedIn = false
        server.enqueue(unauthorized())
        server.enqueue(ok())

        val code = call()

        assertEquals(401, code)
        assertEquals(1, server.requestCount)
    }

    private fun call(): Int {
        val client: OkHttpClient = NetworkFactory.okHttpClient(
            interceptors = listOf(AuthInterceptor(tokenProvider)),
            authenticator = TokenAuthenticator(tokenProvider),
        )
        val request = Request.Builder().url(server.url("/users/me")).build()
        return client.newCall(request).execute().use { it.code }
    }

    private fun ok() = MockResponse.Builder()
        .addHeader("Content-Type", "application/json")
        .body("""{"user_id":"u1"}""")
        .build()

    private fun unauthorized() = MockResponse.Builder()
        .code(401)
        .addHeader("Content-Type", "application/json")
        .body("""{"error":{"code":"auth_required","message":"로그인이 필요합니다. 다시 로그인해 주세요."}}""")
        .build()

    private class FakeIdTokenProvider : IdTokenProvider {
        var loggedIn = true
        val calls = mutableListOf<Boolean>()

        override fun getIdToken(forceRefresh: Boolean): String? {
            calls += forceRefresh
            if (!loggedIn) return null
            return if (forceRefresh) FRESH else CACHED
        }
    }

    private companion object {
        const val CACHED = "cached-token"
        const val FRESH = "fresh-token"
    }
}
