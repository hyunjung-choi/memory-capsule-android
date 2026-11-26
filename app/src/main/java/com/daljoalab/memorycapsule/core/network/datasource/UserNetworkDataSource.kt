package com.daljoalab.memorycapsule.core.network.datasource

import com.daljoalab.memorycapsule.core.model.ApiError
import com.daljoalab.memorycapsule.core.model.ApiException
import com.daljoalab.memorycapsule.core.network.api.UserApi
import com.daljoalab.memorycapsule.core.network.error.ApiCaller
import com.daljoalab.memorycapsule.core.network.model.NetworkUser
import com.daljoalab.memorycapsule.core.network.model.NetworkUserUpsertRequest
import javax.inject.Inject

class UserNetworkDataSource @Inject constructor(
    private val api: UserApi,
    private val apiCaller: ApiCaller,
) {

    suspend fun getMe(): NetworkUser? =
        try {
            apiCaller { api.getMe() }
        } catch (e: ApiException) {
            if (e.error.isNotRegistered()) null else throw e
        }

    suspend fun upsertMe(request: NetworkUserUpsertRequest): NetworkUser =
        apiCaller { api.upsertMe(request) }

    private fun ApiError.isNotRegistered(): Boolean =
        this is ApiError.HttpError && status == 404 && code == NOT_FOUND

    private companion object {
        const val NOT_FOUND = "not_found"
    }
}
