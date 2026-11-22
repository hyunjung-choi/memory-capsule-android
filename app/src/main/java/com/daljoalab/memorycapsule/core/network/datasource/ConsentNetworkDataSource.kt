package com.daljoalab.memorycapsule.core.network.datasource

import com.daljoalab.memorycapsule.core.network.api.ConsentApi
import com.daljoalab.memorycapsule.core.network.error.ApiCaller
import com.daljoalab.memorycapsule.core.network.model.NetworkConsentRequest
import javax.inject.Inject

class ConsentNetworkDataSource @Inject constructor(
    private val api: ConsentApi,
    private val apiCaller: ApiCaller,
) {

    suspend fun saveConsent(request: NetworkConsentRequest) =
        apiCaller.invokeNoContent { api.saveConsent(request) }
}
