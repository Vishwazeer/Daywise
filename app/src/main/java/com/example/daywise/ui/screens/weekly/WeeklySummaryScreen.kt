package com.example.daywise.ui.screens.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daywise.data.local.DaywiseDatabase
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.data.repository.DaywiseRepository
import com.example.daywise.data.remote.GeminiService
import com.example.daywise.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val db = DaywiseDatabase.getInstance(context)
    val prefs = PreferencesManager(context)
    val gemini = GeminiService("") // Key will be taken from prefs inside repository
    val repository = DaywiseRepository(
        db.foodDao(),
        db.exerciseDao(),
        db.weightDao(),
        db.chatMessageDao(),
        prefs,
        gemini
    )

    val viewModel: WeeklySummaryViewModel = viewModel {
        WeeklySummaryViewModel(repository, prefs)
    }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share summary */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // TABS
                ScrollableTabRow(
                    selectedTabIndex = state.selectedWeekIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}
                ) {
                    TAB_LABELS.forEachIndexed { index, label ->
                        Tab(
                            selected = state.selectedWeekIndex == index,
                            onClick = { viewModel.selectWeek(index) },
                            text = { Text(label, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Date Range and Budget Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state.dateRangeText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val budgetText = if (state.isUnderBudget) "under budget" else "over budget"
                            val budgetColor = if (state.isUnderBudget) SuccessGreen else ErrorRed
                            
                            Text(
                                text = "${state.caloriesUnderOver} calories $budgetText this week",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = budgetColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${state.daysTracked} out of 7 days tracked this week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Calories Section
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Table Header
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text("", modifier = Modifier.weight(1.5f))
                                Text("Food", modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Exercise", modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Remaining", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()

                            // Table Rows
                            state.days.forEach { day ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(day.dayLabel, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (day.hasData) "${day.foodCalories}" else "-",
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        if (day.hasData && day.exerciseCalories > 0) "${day.exerciseCalories}" else "-",
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        if (day.hasData) "${day.remainingCalories}" else "-",
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (day.remainingCalories >= 0) SuccessGreen else ErrorRed
                                    )
                                }
                            }
                            HorizontalDivider()

                            // Total Row
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Total", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${state.totalFoodCalories}", modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${state.totalExerciseCalories}", modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${state.totalRemainingCalories}",
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.totalRemainingCalories >= 0) SuccessGreen else ErrorRed
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "* Based on a daily goal of ${state.dailyGoal} calories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Macronutrients Section
                    Text(
                        text = "Macronutrients",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Table Header
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text("", modifier = Modifier.weight(1.5f))
                                Text("Carbs", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Protein", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Fat", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()

                            // Table Rows
                            state.days.forEach { day ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(day.dayLabel, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (day.hasData) "${day.carbsG.toInt()}g" else "-",
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        if (day.hasData) "${day.proteinG.toInt()}g" else "-",
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        if (day.hasData) "${day.fatG.toInt()}g" else "-",
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            HorizontalDivider()

                            // Total Row
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Total", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${state.totalCarbs.toInt()}g", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${state.totalProtein.toInt()}g", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${state.totalFat.toInt()}g", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
