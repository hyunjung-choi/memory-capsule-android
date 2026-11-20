package com.daljoalab.memorycapsule.core.network.auth

import com.daljoalab.memorycapsule.core.network.auth.AuthInterceptor.Companion.HEADER_AUTHORIZATION
import com.daljoalab.memorycapsule.core.network.auth.AuthInterceptor.Companion.bearer
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * 401이면 토큰을 강제 갱신해서 1회만 재시도
 * - 이미 재시도한 응답(priorResponse != null)이면 중단
 * - 로그인 전(갱신 토큰 null)이면 재시도하지 않음
 */
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: IdTokenProvider,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val token = tokenProvider.getIdToken(forceRefresh = true) ?: return null
        return response.request.newBuilder()
            .header(HEADER_AUTHORIZATION, bearer(token))
            .build()
    }
}
