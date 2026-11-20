package com.daljoalab.memorycapsule.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 모든 요청에 `Authorization: Bearer <Firebase ID Token>`을 붙인다. 캐시된 토큰을 쓴다.
 * 로그인 전이면 헤더 없이 보내고, 서버의 `401 auth_required`를 그대로 받는다.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: IdTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getIdToken(forceRefresh = false)
        val request = chain.request().newBuilder()
            .apply { if (token != null) header(HEADER_AUTHORIZATION, bearer(token)) }
            .build()
        return chain.proceed(request)
    }

    internal companion object {
        const val HEADER_AUTHORIZATION = "Authorization"

        fun bearer(token: String) = "Bearer $token"
    }
}
