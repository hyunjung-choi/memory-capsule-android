package com.daljoalab.memorycapsule.feature.guardianhome

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljoalab.memorycapsule.core.designsystem.component.InfoCard
import com.daljoalab.memorycapsule.core.designsystem.component.MemoryScaffold
import com.daljoalab.memorycapsule.core.designsystem.component.PrimaryButton
import com.daljoalab.memorycapsule.core.designsystem.component.SecondaryButton
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme
import com.daljoalab.memorycapsule.core.model.GuardianLink

internal fun String.withRo(): String {
    val last = lastOrNull() ?: return this
    if (last !in '가'..'힣') return "${this}로"
    val finalConsonant = (last - '가') % 28
    return if (finalConsonant == 0 || finalConsonant == RIEUL) "${this}로" else "${this}으로"
}

private const val RIEUL = 8

@Composable
fun GuardianHomeScreen(
    onAddLink: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuardianHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    GuardianHomeScreen(
        uiState = uiState,
        onAddLink = onAddLink,
        onSignOut = {
            viewModel.signOut()
            onSignedOut()
        },
        modifier = modifier,
    )
}

@Composable
private fun GuardianHomeScreen(
    uiState: GuardianHomeUiState,
    onAddLink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MemoryScaffold(
        modifier = modifier,
        title = "연결된 부모님",
        bottomBar = {
            PrimaryButton(text = "부모님 연결하기", onClick = onAddLink)
            SecondaryButton(text = "로그아웃", onClick = onSignOut)
        },
    ) {
        when (val state = uiState) {
            GuardianHomeUiState.Loading -> Text("불러오고 있어요", style = MaterialTheme.typography.bodyLarge)
            is GuardianHomeUiState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
            is GuardianHomeUiState.Loaded -> if (state.links.isEmpty()) {
                InfoCard(
                    title = "아직 연결된 부모님이 없어요",
                    body = "부모님 앱에서 보호자 연결 번호를 받아\n입력해 주세요.",
                )
            } else {
                state.links.forEach { link ->
                    InfoCard(
                        title = link.displayName?.let { "${it}님" } ?: "부모님",
                        body = "${(link.relation ?: "가족").withRo()} 연결돼 있어요.",
                    )
                }
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun GuardianHomeScreenPreview(
    @PreviewParameter(GuardianHomeUiStatePreviewParameterProvider::class) uiState: GuardianHomeUiState,
) {
    MemoryCapsuleTheme {
        GuardianHomeScreen(uiState = uiState, onAddLink = {}, onSignOut = {})
    }
}

private class GuardianHomeUiStatePreviewParameterProvider : PreviewParameterProvider<GuardianHomeUiState> {
    override val values = sequenceOf(
        GuardianHomeUiState.Loading,
        GuardianHomeUiState.Loaded(emptyList()),
        GuardianHomeUiState.Loaded(
            listOf(
                GuardianLink(linkId = "1", seniorId = "s1", displayName = "김영희", relation = "가족"),
                GuardianLink(linkId = "2", seniorId = "s2", displayName = null, relation = "자녀"),
            ),
        ),
        GuardianHomeUiState.Failed("지금은 연결이 어려워요. 잠시 후 다시 해 주세요."),
    )
}
