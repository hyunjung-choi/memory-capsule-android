package com.daljoalab.memorycapsule.core.network.error

import com.daljoalab.memorycapsule.core.model.ApiError
import com.daljoalab.memorycapsule.core.model.ApiException
import com.daljoalab.memorycapsule.core.network.NetworkFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class ApiCallerTest {

    private lateinit var server: MockWebServer
    private val apiCaller = ApiCaller(NetworkFactory.json())

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
    fun `성공 응답은 그대로 돌려준다`() = runTest {
        server.enqueue(response(200, """{"user_id":"u1"}"""))

        val result = apiCaller { createApi().get() }

        assertEquals("u1", result.userId)
    }

    @Test
    fun `4xx 에러 본문의 code와 message를 보존한다`() = runTest {
        server.enqueue(response(403, errorJson("consent_required", "필요한 동의를 먼저 확인해 주세요.")))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(
            ApiError.HttpError(403, "consent_required", "필요한 동의를 먼저 확인해 주세요."),
            error,
        )
    }

    @Test
    fun `429 요청 제한도 HttpError로 구분한다`() = runTest {
        server.enqueue(response(429, errorJson("daily_limit", "오늘은 대화를 더 할 수 없어요.")))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(ApiError.HttpError(429, "daily_limit", "오늘은 대화를 더 할 수 없어요."), error)
    }

    @Test
    fun `5xx 에러 본문도 파싱한다`() = runTest {
        server.enqueue(response(503, errorJson("upstream_unavailable", "잠시 후 다시 해 주세요.")))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(ApiError.HttpError(503, "upstream_unavailable", "잠시 후 다시 해 주세요."), error)
    }

    @Test
    fun `에러 본문이 HTML이면 code와 message 없이 상태 코드만 남긴다`() = runTest {
        server.enqueue(response(502, "<html><body>Bad Gateway</body></html>", "text/html"))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(ApiError.HttpError(502, null, null), error)
    }

    @Test
    fun `에러 본문이 비어 있으면 상태 코드만 남긴다`() = runTest {
        server.enqueue(response(500, ""))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(ApiError.HttpError(500, null, null), error)
    }

    @Test
    fun `에러 본문이 다른 JSON 형식이면 상태 코드만 남긴다`() = runTest {
        server.enqueue(response(400, """{"detail":"bad request"}"""))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(ApiError.HttpError(400, null, null), error)
    }

    @Test
    fun `응답 시간을 넘기면 Timeout`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body("""{"user_id":"u1"}""")
                .headersDelay(1, TimeUnit.SECONDS)
                .build(),
        )
        val client = NetworkFactory.okHttpClient().newBuilder()
            .readTimeout(100, TimeUnit.MILLISECONDS)
            .build()

        val error = captureError { apiCaller { createApi(client).get() } }

        assertEquals(ApiError.Timeout, error)
    }

    @Test
    fun `서버에 연결할 수 없으면 NoConnection`() = runTest {
        val api = createApi()
        server.close()

        val error = captureError { apiCaller { api.get() } }

        assertEquals(ApiError.NoConnection, error)
    }

    @Test
    fun `성공 응답이 예상한 형식과 다르면 InvalidResponse`() = runTest {
        server.enqueue(response(200, """{"unexpected":true}"""))

        val error = captureError { apiCaller { createApi().get() } }

        assertEquals(ApiError.InvalidResponse, error)
    }

    @Test
    fun `204 응답은 예외 없이 끝난다`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        apiCaller.invokeNoContent { createApi().postNoContent() }
    }

    @Test
    fun `본문 없는 API도 실패 응답이면 HttpError`() = runTest {
        server.enqueue(response(409, errorJson("already_linked", "이미 연결되어 있어요.")))

        val error = captureError { apiCaller.invokeNoContent { createApi().postNoContent() } }

        assertEquals(ApiError.HttpError(409, "already_linked", "이미 연결되어 있어요."), error)
    }

    private suspend fun captureError(block: suspend () -> Unit): ApiError {
        try {
            block()
        } catch (e: ApiException) {
            return e.error
        }
        fail("ApiException이 발생해야 함")
        error("unreachable")
    }

    private fun createApi(client: OkHttpClient = NetworkFactory.okHttpClient()): TestApi =
        NetworkFactory.retrofit(server.url("/").toString(), NetworkFactory.json(), client)
            .create(TestApi::class.java)

    private fun response(
        code: Int,
        body: String,
        contentType: String = "application/json",
    ) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", contentType)
        .body(body)
        .build()

    private fun errorJson(code: String, message: String) =
        """{"error":{"code":"$code","message":"$message"}}"""

    private interface TestApi {
        @GET("users/me")
        suspend fun get(): TestResponse

        @POST("consents")
        suspend fun postNoContent(): Response<Unit>
    }

    @Serializable
    private data class TestResponse(@SerialName("user_id") val userId: String)
}
