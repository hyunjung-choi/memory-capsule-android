package com.daljoalab.memorycapsule.core.model

sealed interface ApiError {

    /** [code]·[message]는 본문이 서버 에러 형식이 아니면 null. [message]는 사용자에게 그대로 보여줄 문구 */
    data class HttpError(
        val status: Int,
        val code: String?,
        val message: String?,
    ) : ApiError

    data object Timeout : ApiError

    data object NoConnection : ApiError

    data object InvalidResponse : ApiError
}

class ApiException(
    val error: ApiError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
