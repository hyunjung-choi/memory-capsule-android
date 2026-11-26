package com.daljoalab.memorycapsule.core.model

enum class UserRole {
    SENIOR,
    GUARDIAN,
}

data class User(
    val id: String,
    val role: UserRole,
    val displayName: String?,
    val grantedConsents: Set<ConsentType>,
)

enum class ConsentType(val serverName: String) {
    VOICE_TRANSFER("voice_transfer"),
    TRANSCRIPT_STORE("transcript_store"),
    MEMORY_CARD("memory_card"),
    GUARDIAN_SHARE("guardian_share"),
    SCHEDULE_SHARE("schedule_share"),
    GUARDIAN_TERMS("guardian_terms"),
    GUARDIAN_DATA_VIEW("guardian_data_view"),
    GUARDIAN_SCHEDULE_MANAGE("guardian_schedule_manage"),
    ;

    companion object {
        fun fromServerName(name: String): ConsentType? = entries.firstOrNull { it.serverName == name }
    }
}

data class PairingCode(
    val code: String,
    val validForMillis: Long,
)

data class PairingTarget(
    val seniorId: String,
    val displayName: String?,
)

data class GuardianLink(
    val linkId: String,
    val seniorId: String,
    val displayName: String?,
    val relation: String?,
)
