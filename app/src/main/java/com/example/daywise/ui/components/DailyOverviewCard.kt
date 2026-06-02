package com.example.daywise.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.daywise.theme.CalorieOrange
import com.example.daywise.theme.CarbsAmber
import com.example.daywise.theme.FatPurple
import com.example.daywise.theme.ProteinBlue

/**
 * Data class holding the daily nutrition overview numbers.
 *
 * @param caloriesConsumed Total calories consumed so far today.
 * @param calorieGoal Daily calorie target.
 * @param proteinCurrent Protein consumed in grams.
 * @param proteinGoal Protein goal in grams.
 * @param carbsCurrent Carbs consumed in grams.
 * @param carbsGoal Carbs goal in grams.
 * @param fatCurrent Fat consumed in grams.
 * @param fatGoal Fat goal in grams.
 * @param exerciseCalories Calories burned through exercise.
 */
data class DailyOverviewData(
    val caloriesConsumed: Int = 0,
    val calorieGoal: Int = 2000,
    val proteinCurrent: Float = 0f,
    val proteinGoal: Float = 150f,
    val carbsCurrent: Float = 0f,
    val carbsGoal: Float = 250f,
    val fatCurrent: Float = 0f,
    val fatGoal: Float = 65f,
    val exerciseCalories: Int = 0
)

/**
 * A summary card displayed at the top of the dashboard.
 *
 * Shows a circular calorie progress ring on the left with consumed/goal text
 * in the center. On the right, three compact macro progress bars are stacked
 * vertically for Protein, Carbs, and Fat. Below everything, exercise calories
 * burned are shown.
 *
 * @param data The daily overview numbers.
 * @param modifier Optional modifier for the outer card.
 */
@Composable
fun DailyOverviewCard(
    data: DailyOverviewData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular calorie progress ring
                CalorieRing(
                    consumed = data.caloriesConsumed,
                    goal = data.calorieGoal,
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Macro progress bars
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroProgressBar(
                        label = "Protein",
                        current = data.proteinCurrent,
                        goal = data.proteinGoal,
                        unit = "g",
                        color = ProteinBlue
                    )
                    MacroProgressBar(
                        label = "Carbs",
                        current = data.carbsCurrent,
                        goal = data.carbsGoal,
                        unit = "g",
                        color = CarbsAmber
                    )
                    MacroProgressBar(
                        label = "Fat",
                        current = data.fatCurrent,
                        goal = data.fatGoal,
                        unit = "g",
                        color = FatPurple
                    )
                }
            }

            // Exercise calories section
            if (data.exerciseCalories > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Exercise",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${data.exerciseCalories} kcal burned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * A circular arc / ring showing calorie progress.
 *
 * The track is drawn as a faint orange arc, and the filled portion animates
 * to represent consumed / goal. The consumed and remaining counts are centered.
 */
@Composable
private fun CalorieRing(
    consumed: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "calorieRingProgress"
    )
    val remaining = (goal - consumed).coerceAtLeast(0)

    val trackColor = CalorieOrange.copy(alpha = 0.15f)
    val arcColor = CalorieOrange

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 10.dp.toPx()
            val padding = strokeWidth / 2
            val arcSize = Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            )
            val topLeft = Offset(padding, padding)

            // Track (full circle)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Filled arc
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$consumed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/ $goal kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (remaining > 0) {
                Text(
                    text = "$remaining left",
                    style = MaterialTheme.typography.labelSmall,
                    color = CalorieOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
