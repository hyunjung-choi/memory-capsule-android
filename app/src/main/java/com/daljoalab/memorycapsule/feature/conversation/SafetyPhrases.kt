package com.daljoalab.memorycapsule.feature.conversation

import com.daljoalab.memorycapsule.core.model.RiskLevel

/** 데이터 명세 v0.6 `config_safety`의 후보값. 기획·데이터 검토가 끝나면 확정본으로 변경 */
internal object SafetyPhrases {

    private val phrases = listOf(
        phrase(
            "R2_CHECK_IN", RiskLevel.R2,
            "최근 마음이나 생활에 어려움이 있을 수 있어요. 괜찮으시다면 지금 기분과 필요한 도움을 함께 살펴볼까요?",
        ),
        phrase(
            "R2_SUPPORT", RiskLevel.R2,
            "혼자 감당하지 않으셔도 괜찮아요. 믿을 만한 가족이나 전문가에게 연락해 도움을 요청하실 수 있어요.",
            "1577-0199",
        ),
        phrase(
            "SAFE_SUICIDE_R2", RiskLevel.R2,
            "힘든 마음을 말씀해 주셔서 감사합니다. 혼자 감당하지 않으셔도 됩니다. 지금 안전한 곳에 계신가요? " +
                "도움이 필요하시면 자살예방상담전화 109에 연락할 수 있어요.",
            "109",
        ),
        phrase(
            "SAFE_ANXIETY", RiskLevel.R2,
            "매우 불안하고 힘드시겠어요. 지금 안전한 장소에 계신지 확인해 주세요. " +
                "불안이 계속되면 정신건강상담전화 1577-0199에서 도움을 받을 수 있습니다. " +
                "숨쉬기 어렵거나 심한 가슴 통증이 있다면 119로 연락해 주세요.",
            "1577-0199", "119",
        ),
        phrase(
            "R3_SAFETY_CHECK", RiskLevel.R3,
            "지금 안전이 가장 중요합니다. 현재 안전한 곳에 계신가요? 가까운 믿을 만한 사람에게 바로 도움을 요청해 주세요.",
            "119",
        ),
        phrase(
            "R3_CRISIS_HELP", RiskLevel.R3,
            "지금 위험하다고 느끼신다면 혼자 계시지 마시고 즉시 119에 연락하거나 가까운 응급실에서 도움을 받으세요.",
            "119",
        ),
        phrase(
            "R3_SUICIDE_SUPPORT", RiskLevel.R3,
            "죽음이나 자해에 관한 생각 때문에 힘드시다면 지금 바로 도움을 요청해 주세요. " +
                "자살예방상담전화 109에서 상담받으실 수 있습니다. 즉각적인 위험이 있다면 119에 연락해 주세요.",
            "109", "119",
        ),
        phrase(
            "SAFE_SUICIDE_R3", RiskLevel.R3,
            "지금은 안전을 확인하는 것이 가장 중요합니다. 혼자 계시다면 가까운 사람에게 바로 도움을 요청해 주세요. " +
                "위험이 임박했다면 지금 119에 연락해 주세요. 자살예방상담전화 109에서도 도움을 받을 수 있습니다.",
            "119", "109",
        ),
        phrase(
            "SAFE_CONFUSION", RiskLevel.R3,
            "갑자기 시간이나 장소를 알아보기 어렵거나 평소와 다르게 혼란스럽다면 즉시 의료적 확인이 필요할 수 있습니다. " +
                "지금 119에 연락하고 가까운 사람에게 도움을 요청해 주세요.",
            "119",
        ),
        phrase(
            "SAFE_BEHAVIOR", RiskLevel.R3,
            "지금 신변에 위협이 있다면 우선 안전한 장소로 이동하고 경찰 112에 연락해 주세요. " +
                "다치거나 응급진료가 필요하면 119에 연락해 주세요.",
            "112", "119",
        ),
        phrase(
            "SAFE_MEDICATION", RiskLevel.R3,
            "약을 평소보다 많이 드셨거나 복용 후 의식이 흐려진다면 응급상황일 수 있습니다. 지금 바로 119에 연락해 주세요.",
            "119",
        ),
    ).associateBy { it.phraseId }

    private val phoneNames = mapOf(
        "109" to "자살예방상담전화",
        "119" to "구급 신고",
        "112" to "경찰 신고",
        "1577-0199" to "정신건강상담전화",
    )

    fun noticeFor(phraseId: String?, level: RiskLevel): SafetyNotice? {
        val default = when (level) {
            RiskLevel.R2 -> "R2_CHECK_IN"
            RiskLevel.R3 -> "R3_SAFETY_CHECK"
            else -> return null
        }
        return phrases[phraseId] ?: phrases.getValue(default)
    }

    fun phoneName(number: String): String = phoneNames[number] ?: number

    private fun phrase(id: String, level: RiskLevel, text: String, vararg numbers: String) =
        SafetyNotice(phraseId = id, level = level, text = text, phoneNumbers = numbers.toList())
}
