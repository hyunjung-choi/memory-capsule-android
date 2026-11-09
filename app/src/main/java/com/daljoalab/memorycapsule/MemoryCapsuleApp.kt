package com.daljoalab.memorycapsule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.daljoalab.memorycapsule.navigation.MemoryCapsuleNavHost

@Composable
fun MemoryCapsuleApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    MemoryCapsuleNavHost(
        navController = navController,
        modifier = modifier,
    )
}
