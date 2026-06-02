package com.example.daywise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.ui.screens.chat.ChatScreen
import com.example.daywise.ui.screens.goals.DailyGoalsScreen
import com.example.daywise.ui.screens.onboarding.OnboardingScreen
import com.example.daywise.ui.screens.reminders.RemindersScreen
import com.example.daywise.ui.screens.settings.SettingsScreen
import com.example.daywise.ui.screens.weekly.WeeklySummaryScreen
import com.example.daywise.ui.screens.weight.WeightTrackerScreen
import com.example.daywise.ui.components.DrawerScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current.applicationContext
    val prefs = remember { PreferencesManager(context) }
    val hasCompletedOnboarding by prefs.hasCompletedOnboarding.collectAsState(initial = null)

    if (hasCompletedOnboarding == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (hasCompletedOnboarding == true) Chat else Onboarding
    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        backStack.add(Chat)
                    },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Chat> {
                ChatScreen(
                    onNavigateToDrawer = { destination ->
                        val key = when (destination) {
                            DrawerScreen.DAILY_GOALS -> DailyGoals
                            DrawerScreen.WEEKLY_SUMMARY -> WeeklySummary
                            DrawerScreen.WEIGHT_TRACKER -> WeightTracker
                            DrawerScreen.REMINDERS -> Reminders
                            DrawerScreen.SETTINGS -> Settings
                            else -> null
                        }
                        key?.let { backStack.add(it) }
                    },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<WeeklySummary> {
                WeeklySummaryScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<WeightTracker> {
                WeightTrackerScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<DailyGoals> {
                DailyGoalsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToCalculator = {
                        backStack.add(Onboarding)
                    },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Reminders> {
                RemindersScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Settings> {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
    )
}
