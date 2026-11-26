package com.daljoalab.memorycapsule.core.network.api

import com.daljoalab.memorycapsule.core.network.model.NetworkConsentRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ConsentApi {

    @POST("consents")
    suspend fun saveConsent(@Body request: NetworkConsentRequest): Response<Unit>
}
