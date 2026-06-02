package com.example.daywise.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.daywise.theme.CarbsAmber
import com.example.daywise.theme.FatPurple
import com.example.daywise.theme.ProteinBlue

/**
 * The type of a nutrition log entry.
 */
enum class NutritionEntryType {
    FOOD,
    EXERCISE
}

/**
 * Data class representing a single nutrition log entry (food or exercise).
 *
 * @param id Unique identifier for this entry.
 * @param name Display name (e.g. "Grilled Chicken", "Morning Run").
 * @param type Whether this is a food or exercise entry.
 * @param calories Calories consumed (food) or burned (exercise).
 * @param proteinGrams Protein in grams (food only).
 * @param carbsGrams Carbs in grams (food only).
 * @param fatGrams Fat in grams (food only).
 * @param durationMinutes Duration in minutes (exercise only).
 */
data class NutritionEntry(
    val id: Long,
    val name: String,
    val type: NutritionEntryType,
    val calories: Int,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f,
    val durationMinutes: Int = 0
)

/**
 * A card displaying a logged food or exercise entry with swipe-to-delete support.
 *
 * Food entries show the name, calories, and a P / C / F macro breakdown.
 * Exercise entries show the name, duration, and calories burned.
 *
 * @param entry The nutrition entry data to display.
 * @param onDelete Callback invoked when the user swipes to delete the entry.
 * @param modifier Optional modifier for the outer container.
 */
@Composable
fun NutritionCard(
    entry: NutritionEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState()

    // Trigger the delete callback when the user finishes swiping
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            DismissBackground(dismissState.targetValue)
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            when (entry.type) {
                NutritionEntryType.FOOD -> FoodContent(entry)
                NutritionEntryType.EXERCISE -> ExerciseContent(entry)
            }
        }
    }
}

/**
 * Content layout for a food entry.
 */
@Composable
private fun FoodContent(entry: NutritionEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Restaurant,
            contentDescription = "Food",
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            MacroChips(entry)
        }

        Text(
            text = "${entry.calories} kcal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Small colored text chips showing P / C / F breakdown.
 */
@Composable
private fun MacroChips(entry: NutritionEntry) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroChipText(
            label = "P",
            value = "${entry.proteinGrams.toInt()}g",
            color = ProteinBlue
        )
        MacroChipText(
            label = "C",
            value = "${entry.carbsGrams.toInt()}g",
            color = CarbsAmber
        )
        MacroChipText(
            label = "F",
            value = "${entry.fatGrams.toInt()}g",
            color = FatPurple
        )
    }
}

@Composable
private fun MacroChipText(
    label: String,
    value: String,
    color: Color
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Content layout for an exercise entry.
 */
@Composable
private fun ExerciseContent(entry: NutritionEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsRun,
            contentDescription = "Exercise",
            tint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${entry.durationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "-${entry.calories} kcal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Red background revealed when the user swipes an entry to delete it.
 */
@Composable
private fun DismissBackground(dismissValue: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue = when (dismissValue) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
            else -> Color.Transparent
        },
        label = "dismissBgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onError
            )
        }
    }
}
