package com.daljoalab.memorycapsule.core.network.api

import com.daljoalab.memorycapsule.core.network.model.NetworkUser
import com.daljoalab.memorycapsule.core.network.model.NetworkUserUpsertRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserApi {

    @GET("users/me")
    suspend fun getMe(): NetworkUser

    @POST("users/me")
    suspend fun upsertMe(@Body request: NetworkUserUpsertRequest): NetworkUser
}
