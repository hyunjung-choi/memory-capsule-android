package com.daljoalab.memorycapsule.feature.conversation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.CaptionBubble
import com.daljoalab.memorycapsule.core.designsystem.component.CharacterMotion
import com.daljoalab.memorycapsule.core.designsystem.component.KeepScreenOn
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryCharacter
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SecondaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SpeechBubble
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.ChipBorder
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme
import com.daljoalab.memorycapsule.core.model.Reaction
import com.daljoalab.memorycapsule.core.model.RiskLevel

@Composable
fun ConversationScreen(
    onOpenPairing: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startConversation()
        } else {
            val permanently = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            viewModel.onMicPermissionDenied(permanently)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val waitingForSettings = (uiState.phase as? ConversationPhase.Failed)?.needsMicSettings == true
        if (waitingForSettings && context.hasMicPermission()) viewModel.returnToReady()
    }

    val phase = uiState.phase
    val inSession = phase.isTalking || phase == ConversationPhase.Closing || phase == ConversationPhase.Saving
    BackHandler(enabled = inSession) { viewModel.stopConversation() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stopConversation() }
    KeepScreenOn(enabled = inSession || phase == ConversationPhase.Connecting)

    ConversationScreen(
        uiState = uiState,
        onStart = {
            if (context.hasMicPermission()) {
                viewModel.startConversation()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onOpenMicSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
            )
        },
        onStop = viewModel::stopConversation,
        onNavigateHome = viewModel::returnToReady,
        onOpenPairing = onOpenPairing,
        onCall = { number ->
            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
        },
        onDismissSafetyNotice = viewModel::dismissSafetyNotice,
        modifier = modifier,
    )
}

@Composable
internal fun ConversationScreen(
    uiState: ConversationUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenMicSettings: () -> Unit,
    onNavigateHome: () -> Unit,
    onOpenPairing: () -> Unit,
    onCall: (phoneNumber: String) -> Unit,
    onDismissSafetyNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val phase = uiState.phase
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (phase.canStart) PairingChip(onClick = onOpenPairing)
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                SpeechBubble(text = uiState.bubbleText())
                MemoryCharacter(
                    size = phase.characterSize,
                    motion = uiState.reaction.toMotion(),
                    speaking = phase == ConversationPhase.Answering,
                    contentDescription = if (phase.canStart) "기억이와 이야기하기" else "기억이",
                    onClick = if (phase.canStart) onStart else null,
                )
                if (phase == ConversationPhase.Listening) {
                    ListeningLabel()
                    if (uiState.seniorText.isNotBlank()) {
                        CaptionBubble(
                            text = uiState.seniorText.lastLines(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                uiState.safetyNotice?.let {
                    SafetyNoticeCard(notice = it, onCall = onCall, onDismiss = onDismissSafetyNotice)
                }
            }
            BottomActions(
                phase = phase,
                onStart = onStart,
                onStop = onStop,
                onOpenMicSettings = onOpenMicSettings,
                onNavigateHome = onNavigateHome,
            )
        }
    }
}

@Composable
private fun PairingChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, ChipBorder),
    ) {
        Text(
            text = "보호자 연결",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ListeningLabel() {
    Text(
        text = "•  듣는 중",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BottomActions(
    phase: ConversationPhase,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenMicSettings: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (phase) {
            ConversationPhase.Ready -> PrimaryButton(text = "기억이와 이야기하기", onClick = onStart)
            ConversationPhase.Listening, ConversationPhase.Answering ->
                SecondaryButton(text = "대화 마치기", onClick = onStop)
            is ConversationPhase.Failed -> {
                if (phase.needsMicSettings) {
                    PrimaryButton(text = "설정에서 마이크 켜기", onClick = onOpenMicSettings)
                } else {
                    PrimaryButton(text = "다시 이야기하기", onClick = onStart)
                }
                SecondaryButton(text = "홈으로 가기", onClick = onNavigateHome)
            }
            is ConversationPhase.Finished -> {
                PrimaryButton(text = "다시 이야기하기", onClick = onStart)
                SecondaryButton(text = "홈으로 가기", onClick = onNavigateHome)
            }
            ConversationPhase.Connecting, ConversationPhase.Closing, ConversationPhase.Saving -> Unit
        }
    }
}

@Composable
private fun SafetyNoticeCard(
    notice: SafetyNotice,
    onCall: (phoneNumber: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val urgent = notice.level == RiskLevel.R3
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = if (urgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (urgent) "지금 안전이 가장 중요해요" else "마음을 살펴 드릴게요",
                style = MaterialTheme.typography.titleMedium,
                color = if (urgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = notice.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            notice.phoneNumbers.forEach { number ->
                PrimaryButton(
                    text = "${SafetyPhrases.phoneName(number)} $number",
                    onClick = { onCall(number) },
                )
            }
            SecondaryButton(text = "괜찮아요", onClick = onDismiss)
        }
    }
}

private fun Context.hasMicPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private val ConversationPhase.canStart: Boolean
    get() = this == ConversationPhase.Ready || this is ConversationPhase.Finished || this is ConversationPhase.Failed

private val ConversationPhase.characterSize
    get() = when (this) {
        ConversationPhase.Ready -> 264.dp
        ConversationPhase.Answering -> 232.dp
        is ConversationPhase.Failed -> 104.dp
        else -> 180.dp
    }

private fun ConversationUiState.bubbleText(): String = when (val phase = phase) {
    ConversationPhase.Ready -> "오늘 어떠셨어요?"
    ConversationPhase.Connecting -> "잠시만 기다려 주세요"
    ConversationPhase.Listening -> assistantText.ifBlank { "오늘 어떠셨어요?" }
    ConversationPhase.Answering -> assistantText.ifBlank { "..." }
    ConversationPhase.Closing -> "대화를 마무리하고 있어요"
    ConversationPhase.Saving -> "오늘 이야기를\n기억 카드로 정리하고 있어요"
    is ConversationPhase.Finished -> when (phase.cardsCreated) {
        null -> "이야기를 저장하지 못했어요.\n다음에 다시 들려주세요."
        0 -> "이야기 들려주셔서\n고마워요."
        else -> "기억 카드 ${phase.cardsCreated}장을\n저장했어요."
    }
    is ConversationPhase.Failed -> phase.message
}

private fun String.lastLines(): String =
    if (length <= MAX_CAPTION_CHARS) this else "…" + takeLast(MAX_CAPTION_CHARS)

private const val MAX_CAPTION_CHARS = 48

private fun Reaction.toMotion(): CharacterMotion = when (this) {
    Reaction.NEUTRAL -> CharacterMotion.CALM
    Reaction.HAPPY -> CharacterMotion.JOYFUL
    Reaction.EMPATHY -> CharacterMotion.GENTLE
    Reaction.SURPRISED_HAPPY, Reaction.SURPRISED_CONCERN -> CharacterMotion.SURPRISED
}

@ScreenPreviews
@Composable
private fun ConversationScreenPreview(
    @PreviewParameter(ConversationUiStatePreviewParameterProvider::class) uiState: ConversationUiState,
) {
    MemoryCapsuleTheme {
        ConversationScreen(
            uiState = uiState,
            onStart = {},
            onStop = {},
            onOpenMicSettings = {},
            onNavigateHome = {},
            onOpenPairing = {},
            onCall = {},
            onDismissSafetyNotice = {},
        )
    }
}

private class ConversationUiStatePreviewParameterProvider : PreviewParameterProvider<ConversationUiState> {
    override val values = sequenceOf(
        ConversationUiState(),
        ConversationUiState(phase = ConversationPhase.Connecting),
        ConversationUiState(
            phase = ConversationPhase.Listening,
            assistantText = "오늘 어떠셨어요?",
            seniorText = "오늘은 조금 외로웠어요. 아침에 혼자 밥을 먹고 창밖을 한참 바라봤어요.",
        ),
        ConversationUiState(
            phase = ConversationPhase.Answering,
            reaction = Reaction.HAPPY,
            assistantText = "마음이 편해지셨다니 다행이에요.",
        ),
        ConversationUiState(
            phase = ConversationPhase.Listening,
            reaction = Reaction.EMPATHY,
            assistantText = "즐거운 시간 뒤에 더 허전하게 느껴질 때가 있죠.",
            safetyNotice = SafetyPhrases.noticeFor("R2_SUPPORT", RiskLevel.R2),
        ),
        ConversationUiState(
            phase = ConversationPhase.Listening,
            reaction = Reaction.EMPATHY,
            assistantText = "말씀해 주셔서 고마워요.",
            safetyNotice = SafetyPhrases.noticeFor("R3_SUICIDE_SUPPORT", RiskLevel.R3),
        ),
        ConversationUiState(phase = ConversationPhase.Saving),
        ConversationUiState(phase = ConversationPhase.Finished(cardsCreated = 3)),
        ConversationUiState(phase = ConversationPhase.Finished(cardsCreated = null)),
        ConversationUiState(phase = ConversationPhase.Failed("답변을 가져오지 못했어요. 인터넷 연결을 확인하고 다시 이야기해 주세요.")),
        ConversationUiState(
            phase = ConversationPhase.Failed("마이크가 꺼져 있어요.\n설정에서 마이크를 허용해 주세요.", needsMicSettings = true),
        ),
    )
}
