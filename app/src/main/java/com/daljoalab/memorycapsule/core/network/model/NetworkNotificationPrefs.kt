package com.daljoalab.memorycapsule.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkNotificationPrefs(
    @SerialName("risk_min_level") val riskMinLevel: String? = null,
    @SerialName("schedule_missed") val scheduleMissed: Boolean? = null,
    @SerialName("weekly_report") val weeklyReport: Boolean? = null,
    @SerialName("quiet_hours") val quietHours: NetworkQuietHours? = null,
)

@Serializable
data class NetworkQuietHours(
    val from: String? = null,
    val to: String? = null,
)
