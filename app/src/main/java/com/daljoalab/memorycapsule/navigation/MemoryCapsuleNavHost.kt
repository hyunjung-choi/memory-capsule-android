package com.daljoalab.memorycapsule.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.daljoalab.memorycapsule.feature.conversation.ConversationScreen
import com.daljoalab.memorycapsule.feature.guardianauth.GuardianAuthResult
import com.daljoalab.memorycapsule.feature.guardianauth.GuardianLoginScreen
import com.daljoalab.memorycapsule.feature.guardianauth.GuardianProfileScreen
import com.daljoalab.memorycapsule.feature.guardianauth.GuardianSignUpScreen
import com.daljoalab.memorycapsule.feature.guardianhome.GuardianHomeScreen
import com.daljoalab.memorycapsule.feature.guardianlink.GuardianLinkScreen
import com.daljoalab.memorycapsule.feature.launch.LaunchDestination
import com.daljoalab.memorycapsule.feature.launch.LaunchScreen
import com.daljoalab.memorycapsule.feature.roleselect.RoleSelectScreen
import com.daljoalab.memorycapsule.feature.senioronboarding.SeniorOnboardingScreen
import com.daljoalab.memorycapsule.feature.seniorpairing.SeniorPairingScreen

@Composable
fun MemoryCapsuleNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LaunchRoute,
        modifier = modifier,
    ) {
        composable<LaunchRoute> {
            LaunchScreen(
                onDecided = { destination ->
                    navController.navigateAsRoot(
                        when (destination) {
                            LaunchDestination.ROLE_SELECT -> RoleSelectRoute
                            LaunchDestination.SENIOR_ONBOARDING -> SeniorOnboardingRoute
                            LaunchDestination.SENIOR_HOME -> ConversationRoute
                            LaunchDestination.GUARDIAN_PROFILE -> GuardianProfileRoute
                            LaunchDestination.GUARDIAN_HOME -> GuardianHomeRoute
                        },
                    )
                },
            )
        }
        composable<RoleSelectRoute> {
            RoleSelectScreen(
                onSeniorSelected = { navController.navigate(SeniorOnboardingRoute) },
                onGuardianSelected = { navController.navigate(GuardianLoginRoute) },
            )
        }

        composable<SeniorOnboardingRoute> {
            SeniorOnboardingScreen(
                onBack = { navController.backOrRoleSelect() },
                onCompleted = { navController.navigateAsRoot(ConversationRoute) },
            )
        }
        composable<ConversationRoute> {
            ConversationScreen(onOpenPairing = { navController.navigate(SeniorPairingRoute) })
        }
        composable<SeniorPairingRoute> {
            SeniorPairingScreen(onNavigateHome = { navController.popBackStack() })
        }

        val onGuardianAuthResult = { result: GuardianAuthResult ->
            navController.navigateAsRoot(
                when (result) {
                    GuardianAuthResult.NEEDS_PROFILE -> GuardianProfileRoute
                    GuardianAuthResult.SIGNED_IN -> GuardianHomeRoute
                },
            )
        }
        composable<GuardianLoginRoute> {
            GuardianLoginScreen(
                onBack = { navController.backOrRoleSelect() },
                onSignUp = { navController.navigate(GuardianSignUpRoute) },
                onResult = onGuardianAuthResult,
            )
        }
        composable<GuardianSignUpRoute> {
            GuardianSignUpScreen(
                onBack = { navController.popBackStack() },
                onResult = onGuardianAuthResult,
            )
        }
        composable<GuardianProfileRoute> {
            GuardianProfileScreen(
                onBack = { navController.backOrRoleSelect() },
                onCompleted = {
                    navController.navigateAsRoot(GuardianHomeRoute)
                    navController.navigate(GuardianLinkRoute)
                },
            )
        }
        composable<GuardianHomeRoute> {
            GuardianHomeScreen(
                onAddLink = { navController.navigate(GuardianLinkRoute) },
                onSignedOut = { navController.navigateAsRoot(RoleSelectRoute) },
            )
        }
        composable<GuardianLinkRoute> {
            GuardianLinkScreen(
                onBack = { navController.popBackStack() },
                onLinked = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.navigateAsRoot(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.backOrRoleSelect() {
    if (!popBackStack()) navigateAsRoot(RoleSelectRoute)
}
