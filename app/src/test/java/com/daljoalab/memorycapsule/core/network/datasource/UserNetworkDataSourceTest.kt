package com.daljoalab.memorycapsule.core.network.datasource

import com.daljoalab.memorycapsule.core.model.ApiError
import com.daljoalab.memorycapsule.core.model.ApiException
import com.daljoalab.memorycapsule.core.network.NetworkFactory
import com.daljoalab.memorycapsule.core.network.api.UserApi
import com.daljoalab.memorycapsule.core.network.error.ApiCaller
import com.daljoalab.memorycapsule.core.network.model.NetworkConsentState
import com.daljoalab.memorycapsule.core.network.model.NetworkNotificationPrefs
import com.daljoalab.memorycapsule.core.network.model.NetworkQuietHours
import com.daljoalab.memorycapsule.core.network.model.NetworkUser
import com.daljoalab.memorycapsule.core.network.model.NetworkUserRole
import com.daljoalab.memorycapsule.core.network.model.NetworkUserUpsertRequest
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class UserNetworkDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: UserNetworkDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = NetworkFactory.json()
        val api = NetworkFactory.retrofit(server.url("/").toString(), json, NetworkFactory.okHttpClient())
            .create(UserApi::class.java)
        dataSource = UserNetworkDataSource(api, ApiCaller(json))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `프로필 조회는 GET users_me로 요청한다`() = runTest {
        server.enqueue(response(200, SENIOR_PROFILE))

        dataSource.getMe()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/users/me", request.url.encodedPath)
    }

    @Test
    fun `프로필 응답을 파싱한다`() = runTest {
        server.enqueue(response(200, GUARDIAN_PROFILE))

        val user = dataSource.getMe()

        assertEquals(
            NetworkUser(
                userId = "guardian_1",
                role = NetworkUserRole.GUARDIAN,
                displayName = "영희",
                timezone = "Asia/Seoul",
                notificationPrefs = NetworkNotificationPrefs(
                    riskMinLevel = "R2",
                    scheduleMissed = true,
                    weeklyReport = false,
                    quietHours = NetworkQuietHours(from = "22:00", to = "07:00"),
                ),
                consents = listOf(
                    NetworkConsentState(
                        type = "guardian_data_view",
                        version = "test-v1",
                        granted = true,
                        method = "screen_tap",
                        linkedSeniorId = "senior_1",
                        grantedAt = "2026-11-20T01:00:00Z",
                        revokedAt = null,
                        updatedAt = "2026-11-20T01:00:00Z",
                    ),
                ),
                guardianCount = 0,
            ),
            user,
        )
    }

    @Test
    fun `이름과 보호자 설정이 null이어도 파싱한다`() = runTest {
        server.enqueue(response(200, SENIOR_PROFILE))

        val user = dataSource.getMe()

        assertNull(user?.displayName)
        assertNull(user?.notificationPrefs)
        assertEquals(NetworkUserRole.SENIOR, user?.role)
    }

    @Test
    fun `회원 등록 전이면 null을 돌려준다`() = runTest {
        server.enqueue(response(404, errorJson("not_found", "요청한 정보를 찾을 수 없습니다.")))

        assertNull(dataSource.getMe())
    }

    @Test
    fun `회원 등록 전이 아닌 실패는 그대로 던진다`() = runTest {
        server.enqueue(response(401, errorJson("auth_required", "로그인이 필요합니다. 다시 로그인해 주세요.")))

        val error = captureError { dataSource.getMe() }

        assertEquals(ApiError.HttpError(401, "auth_required", "로그인이 필요합니다. 다시 로그인해 주세요."), error)
    }

    @Test
    fun `최초 등록 요청은 role과 timezone만 보낸다`() = runTest {
        server.enqueue(response(200, SENIOR_PROFILE))

        dataSource.upsertMe(NetworkUserUpsertRequest(role = NetworkUserRole.SENIOR, timezone = "Asia/Seoul"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/users/me", request.url.encodedPath)
        assertEquals("""{"role":"senior","timezone":"Asia/Seoul"}""", request.body?.utf8())
    }

    @Test
    fun `수정 요청은 바꿀 필드만 보낸다`() = runTest {
        server.enqueue(response(200, GUARDIAN_PROFILE))

        dataSource.upsertMe(
            NetworkUserUpsertRequest(
                notificationPrefs = NetworkNotificationPrefs(quietHours = NetworkQuietHours(from = "23:00")),
            ),
        )

        assertEquals(
            """{"notification_prefs":{"quiet_hours":{"from":"23:00"}}}""",
            server.takeRequest().body?.utf8(),
        )
    }

    @Test
    fun `역할을 바꾸려 하면 role_immutable 에러를 던진다`() = runTest {
        server.enqueue(response(409, errorJson("role_immutable", "처음 선택한 역할은 바꿀 수 없습니다.")))

        val error = captureError {
            dataSource.upsertMe(NetworkUserUpsertRequest(role = NetworkUserRole.GUARDIAN))
        }

        assertEquals(ApiError.HttpError(409, "role_immutable", "처음 선택한 역할은 바꿀 수 없습니다."), error)
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

    private fun response(code: Int, body: String) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun errorJson(code: String, message: String) =
        """{"error":{"code":"$code","message":"$message"}}"""

    private companion object {
        val SENIOR_PROFILE = """
            {
              "user_id": "senior_1",
              "role": "senior",
              "display_name": null,
              "timezone": "Asia/Seoul",
              "notification_prefs": null,
              "consents": [],
              "guardian_count": 1
            }
        """.trimIndent()

        val GUARDIAN_PROFILE = """
            {
              "user_id": "guardian_1",
              "role": "guardian",
              "display_name": "영희",
              "timezone": "Asia/Seoul",
              "notification_prefs": {
                "risk_min_level": "R2",
                "schedule_missed": true,
                "weekly_report": false,
                "quiet_hours": { "from": "22:00", "to": "07:00" }
              },
              "consents": [
                {
                  "consent_id": "c_1",
                  "type": "guardian_data_view",
                  "version": "test-v1",
                  "granted": true,
                  "method": "screen_tap",
                  "role": "guardian",
                  "linked_senior_id": "senior_1",
                  "granted_at": "2026-11-20T01:00:00Z",
                  "revoked_at": null,
                  "created_at": "2026-11-20T01:00:00Z",
                  "updated_at": "2026-11-20T01:00:00Z",
                  "schema_version": "1"
                }
              ],
              "guardian_count": 0
            }
        """.trimIndent()
    }
}
