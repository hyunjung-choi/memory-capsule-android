package com.daljoalab.memorycapsule.core.network.error

import kotlinx.serialization.Serializable

@Serializable
internal data class NetworkErrorResponse(val error: ErrorBody) {

    @Serializable
    data class ErrorBody(
        val code: String? = null,
        val message: String? = null,
    )
}
