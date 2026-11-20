package com.daljoalab.memorycapsule.core.network.auth

/**
 * 서버 요청에 붙일 Firebase ID Token을 제공
 * OkHttp 스레드에서 동기로 호출되므로 메인 스레드에서 부르면 안됨
 */
interface IdTokenProvider {
    /** 로그인 전이거나 발급에 실패하면 null */
    fun getIdToken(forceRefresh: Boolean): String?
}
