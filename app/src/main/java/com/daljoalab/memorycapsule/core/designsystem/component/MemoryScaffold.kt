package com.daljoalab.memorycapsule.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daljoalab.memorycapsule.core.designsystem.preview.ScreenPreviews
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

@Composable
fun MemoryScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Text("‹ 이전", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (title != null) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                }
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = bottomBar)
        }
    }
}

@ScreenPreviews
@Composable
private fun MemoryScaffoldPreview() {
    MemoryCapsuleTheme {
        MemoryScaffold(
            title = "화면 제목",
            onBack = {},
            bottomBar = {
                PrimaryButton(text = "다음", onClick = {})
                SecondaryButton(text = "나중에", onClick = {})
            },
        ) {
            InfoCard(title = "본문", body = "본문이 길면 이 영역만 스크롤돼요.")
        }
    }
}
