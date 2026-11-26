package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUserUpsertRequest(
    val role: NetworkUserRole? = null,
    @SerialName("display_name") val displayName: String? = null,
    val timezone: String? = null,
    @SerialName("notification_prefs") val notificationPrefs: NetworkNotificationPrefs? = null,
)
