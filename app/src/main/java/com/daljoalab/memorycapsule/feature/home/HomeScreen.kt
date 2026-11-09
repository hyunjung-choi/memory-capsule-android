package com.daljoalab.memorycapsule.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.daljoalab.memorycapsule.R
import com.daljoalab.memorycapsule.core.designsystem.theme.MemoryCapsuleTheme

// 임시 시작 화면
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.app_name))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MemoryCapsuleTheme {
        HomeScreen()
    }
}
