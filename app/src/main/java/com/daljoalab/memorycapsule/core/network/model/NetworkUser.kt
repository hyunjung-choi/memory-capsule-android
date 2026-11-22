package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkUser(
    @SerialName("user_id") val userId: String,
    val role: NetworkUserRole,
    @SerialName("display_name") val displayName: String? = null,
    val timezone: String,
    @SerialName("notification_prefs") val notificationPrefs: NetworkNotificationPrefs? = null,
    val consents: List<NetworkConsentState> = emptyList(),
    @SerialName("guardian_count") val guardianCount: Int = 0,
)

@Serializable
enum class NetworkUserRole {
    @SerialName("senior") SENIOR,
    @SerialName("guardian") GUARDIAN,
}
