package com.daljoalab.memorycapsule.core.model

class UserFacingException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun Throwable.toUserMessage(): String = when (this) {
    is UserFacingException -> message ?: DEFAULT_MESSAGE
    is ApiException -> when (val error = error) {
        is ApiError.HttpError -> error.message ?: DEFAULT_MESSAGE
        ApiError.NoConnection -> "인터넷 연결을 확인해 주세요."
        ApiError.Timeout -> "응답이 늦어지고 있어요. 잠시 후 다시 해 주세요."
        ApiError.InvalidResponse -> DEFAULT_MESSAGE
    }
    else -> DEFAULT_MESSAGE
}

private const val DEFAULT_MESSAGE = "지금은 연결이 어려워요. 잠시 후 다시 해 주세요."
