package com.example.daywise.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * A horizontal scrollable strip showing days of the current week.
 *
 * Each day is rendered as a circular cell with the day abbreviation and date number.
 * The selected day gets a filled primary color background, and today is marked
 * with a small dot indicator beneath the date.
 *
 * @param selectedDate The currently selected date.
 * @param onDateSelected Callback invoked when the user taps a day circle.
 * @param modifier Optional modifier for the outer container.
 */
@Composable
fun CalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    // Build the week containing the selected date (Monday–Sunday)
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val weekDays = remember(weekStart) {
        (0L..6L).map { weekStart.plusDays(it) }
    }

    val listState = rememberLazyListState()

    // Scroll to the selected day when the week changes
    LaunchedEffect(weekStart) {
        val index = weekDays.indexOf(selectedDate)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(weekDays, key = { it.toEpochDay() }) { date ->
            DayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

/**
 * A single circular day cell inside [CalendarStrip].
 */
@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "dayBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dayContentColor"
    )

    val dayAbbreviation = date.dayOfWeek
        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
        .take(3)

    Column(
        modifier = modifier
            .width(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Day abbreviation (Mon, Tue, etc.)
        Text(
            text = dayAbbreviation,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Date number inside a circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Today dot indicator
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            // Invisible spacer to maintain consistent height
            Spacer(modifier = Modifier.size(6.dp))
        }
    }
}
