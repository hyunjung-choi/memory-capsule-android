package com.daljoalab.memorycapsule.core.network.datasource

import com.daljoalab.memorycapsule.core.network.api.ConversationApi
import com.daljoalab.memorycapsule.core.network.error.ApiCaller
import com.daljoalab.memorycapsule.core.network.model.NetworkDecision
import com.daljoalab.memorycapsule.core.network.model.NetworkDecisionRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionCreateRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionCreateResponse
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionEndRequest
import com.daljoalab.memorycapsule.core.network.model.NetworkSessionEndResponse
import javax.inject.Inject

class ConversationNetworkDataSource @Inject constructor(
    private val api: ConversationApi,
    private val apiCaller: ApiCaller,
) {

    /** 과금되므로 실패해도 자동 재시도하지 않는다 */
    suspend fun createSession(request: NetworkSessionCreateRequest): NetworkSessionCreateResponse =
        apiCaller { api.createSession(request) }

    suspend fun decide(request: NetworkDecisionRequest): NetworkDecision =
        apiCaller { api.decide(request) }

    suspend fun endSession(sessionId: String, request: NetworkSessionEndRequest): NetworkSessionEndResponse =
        apiCaller { api.endSession(sessionId, request) }
}
