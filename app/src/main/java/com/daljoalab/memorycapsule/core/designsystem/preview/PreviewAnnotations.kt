package com.daljoalab.memorycapsule.core.designsystem.preview

import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "기본", showBackground = true, backgroundColor = PAPER, widthDp = 390, heightDp = 844)
@Preview(name = "큰 글자", showBackground = true, backgroundColor = PAPER, widthDp = 390, heightDp = 844, fontScale = 1.3f)
annotation class ScreenPreviews

@Preview(name = "기본", showBackground = true, backgroundColor = PAPER, widthDp = 390)
@Preview(name = "큰 글자", showBackground = true, backgroundColor = PAPER, widthDp = 390, fontScale = 1.3f)
annotation class ComponentPreviews

private const val PAPER = 0xFFFBFCF9
