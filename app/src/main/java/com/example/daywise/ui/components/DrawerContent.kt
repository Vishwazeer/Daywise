package com.example.daywise.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Screen identifiers for drawer navigation destinations.
 */
object DrawerScreen {
    const val DAILY_GOALS = "daily_goals"
    const val WEEKLY_SUMMARY = "weekly_summary"
    const val WEIGHT_TRACKER = "weight_tracker"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

/**
 * Navigation drawer content matching Journable's sidebar style.
 *
 * Displays the "Daywise" brand header with a leaf icon, followed by
 * navigation items for Daily Goals, Weekly Summary, Weight Tracker,
 * Reminders, and Settings. A divider separates the main items from
 * the bottom About section.
 *
 * @param selectedScreen The currently selected screen identifier (from [DrawerScreen]).
 * @param onNavigate Callback invoked with the screen identifier when a menu item is tapped.
 * @param modifier Optional modifier for the outer column.
 */
@Composable
fun DrawerContent(
    selectedScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // App header
        DrawerHeader()

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Main navigation items
        DrawerMenuItem(
            icon = Icons.Default.Flag,
            label = "Daily Goals",
            selected = selectedScreen == DrawerScreen.DAILY_GOALS,
            onClick = { onNavigate(DrawerScreen.DAILY_GOALS) }
        )
        DrawerMenuItem(
            icon = Icons.Default.BarChart,
            label = "Weekly Summary",
            selected = selectedScreen == DrawerScreen.WEEKLY_SUMMARY,
            onClick = { onNavigate(DrawerScreen.WEEKLY_SUMMARY) }
        )
        DrawerMenuItem(
            icon = Icons.Default.MonitorWeight,
            label = "Weight Tracker",
            selected = selectedScreen == DrawerScreen.WEIGHT_TRACKER,
            onClick = { onNavigate(DrawerScreen.WEIGHT_TRACKER) }
        )
        DrawerMenuItem(
            icon = Icons.Default.Notifications,
            label = "Reminders",
            selected = selectedScreen == DrawerScreen.REMINDERS,
            onClick = { onNavigate(DrawerScreen.REMINDERS) }
        )
        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = selectedScreen == DrawerScreen.SETTINGS,
            onClick = { onNavigate(DrawerScreen.SETTINGS) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom section
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        DrawerMenuItem(
            icon = Icons.Default.Info,
            label = "About",
            selected = selectedScreen == DrawerScreen.ABOUT,
            onClick = { onNavigate(DrawerScreen.ABOUT) }
        )
    }
}

/**
 * Brand header showing the app name "Daywise" with a leaf (Spa) icon.
 */
@Composable
private fun DrawerHeader() {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        label = {
            Text(
                text = "Daywise",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        selected = false,
        onClick = {},
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

/**
 * A single drawer menu item.
 */
@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
