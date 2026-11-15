package com.daljoalab.memorycapsule.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

class NetworkFactoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `BASE_URL의 경로 뒤에 API 경로가 붙는다`() = runTest {
        server.enqueue(jsonResponse("""{"user_id":"u1"}"""))

        createApi(baseUrl = server.url("/api/").toString()).get()

        assertEquals("/api/users/me", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `응답에 모르는 필드가 있어도 파싱된다`() = runTest {
        server.enqueue(jsonResponse("""{"user_id":"u1","new_field":true}"""))

        val result = createApi().get()

        assertEquals("u1", result.userId)
    }

    @Test
    fun `null 필드는 요청 본문에서 빠진다`() = runTest {
        server.enqueue(jsonResponse("""{"user_id":"u1"}"""))

        createApi().post(TestRequest(role = "senior", nickname = null))

        assertEquals(
            """{"role":"senior","platform":"android"}""",
            server.takeRequest().body?.utf8()
        )
    }

    @Test
    fun `기본값이 있는 필드도 요청 본문에 들어간다`() = runTest {
        server.enqueue(jsonResponse("""{"user_id":"u1"}"""))

        createApi().post(TestRequest(role = "guardian"))

        assertEquals(
            """{"role":"guardian","platform":"android"}""",
            server.takeRequest().body?.utf8()
        )
    }

    @Test
    fun `로그에서 Authorization 헤더가 가려진다`() = runTest {
        val logs = mutableListOf<String>()
        val addToken = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder().header("Authorization", "Bearer $TOKEN").build(),
            )
        }
        // 실제 앱처럼 토큰을 붙인 뒤에 로깅
        val client = NetworkFactory.okHttpClient(
            interceptors = listOf(addToken, NetworkFactory.loggingInterceptor { logs += it }),
        )
        server.enqueue(jsonResponse("""{"user_id":"u1"}"""))

        createApi(client = client).get()

        assertTrue(logs.any { it.startsWith("Authorization:") })
        assertFalse(logs.any { TOKEN in it })
    }

    private fun createApi(
        baseUrl: String = server.url("/").toString(),
        client: OkHttpClient = NetworkFactory.okHttpClient(),
    ): TestApi = NetworkFactory.retrofit(baseUrl, NetworkFactory.json(), client)
        .create(TestApi::class.java)

    private fun jsonResponse(body: String) = MockResponse.Builder()
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private interface TestApi {
        @GET("users/me")
        suspend fun get(): TestResponse

        @POST("users/me")
        suspend fun post(@Body body: TestRequest): TestResponse
    }

    @Serializable
    private data class TestResponse(@SerialName("user_id") val userId: String)

    @Serializable
    private data class TestRequest(
        val role: String,
        val nickname: String? = null,
        val platform: String = "android",
    )

    private companion object {
        const val TOKEN = "secret-id-token"
    }
}
