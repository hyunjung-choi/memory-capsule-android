package com.daljoalab.memorycapsule.core.network.api

import com.daljoalab.memorycapsule.core.network.model.NetworkDecision
import com.daljoalab.memorycapsule.core.network.model.NetworkDecisionRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionCreateRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionCreateResponse
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionEndRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionEndResponse
import com.daljoalab.memorycapsule.core.network.timeout.ReadTimeout
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ConversationApi {

    @ReadTimeout(seconds = 25)
    @POST("api/session")
    suspend fun createSession(@Body request: NetworkSessionCreateRequest): NetworkSessionCreateResponse

    @POST("api/decision")
    suspend fun decide(@Body request: NetworkDecisionRequest): NetworkDecision

    @ReadTimeout(seconds = 75)
    @POST("sessions/{id}/end")
    suspend fun endSession(
        @Path("id") sessionId: String,
        @Body request: NetworkSessionEndRequest,
    ): NetworkSessionEndResponse
}
