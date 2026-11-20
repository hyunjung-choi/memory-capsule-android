package com.daljoalab.memorycapsule.core.network.error

import com.daljoalab.memorycapsule.core.model.ApiError
import com.daljoalab.memorycapsule.core.model.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiCaller @Inject constructor(
    private val json: Json,
) {

    suspend operator fun <T> invoke(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw ApiException(e.response().toHttpError(e.code()), e)
        } catch (e: InterruptedIOException) {
            throw ApiException(ApiError.Timeout, e)
        } catch (e: IOException) {
            throw ApiException(ApiError.NoConnection, e)
        } catch (e: SerializationException) {
            throw ApiException(ApiError.InvalidResponse, e)
        }

    /** `Response<T>`를 반환하는 API는 Retrofit이 실패 응답에도 예외를 던지지 않아서 직접 검사한다 */
    suspend fun invokeNoContent(block: suspend () -> Response<Unit>) {
        val response = invoke(block)
        if (!response.isSuccessful) {
            throw ApiException(response.toHttpError(response.code()), HttpException(response))
        }
    }

    private fun Response<*>?.toHttpError(status: Int): ApiError.HttpError {
        val body = this?.errorBody()?.use { runCatching { it.string() }.getOrNull() }
        val parsed = body
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<NetworkErrorResponse>(it) }.getOrNull() }
            ?.error
        return ApiError.HttpError(
            status = status,
            code = parsed?.code?.takeIf { it.isNotBlank() },
            message = parsed?.message?.takeIf { it.isNotBlank() },
        )
    }
}
