package com.example.daywise.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyGoalsScreen(
    onBack: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val prefs = PreferencesManager(context)
    val viewModel: GoalsViewModel = viewModel {
        GoalsViewModel(prefs)
    }

    val state by viewModel.uiState.collectAsState()

    var calorieInput by remember { mutableStateOf("") }
    var carbsInput by remember { mutableStateOf("") }
    var proteinInput by remember { mutableStateOf("") }
    var fatInput by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (calorieInput.isEmpty()) calorieInput = state.calorieGoal.toString()
        if (carbsInput.isEmpty()) carbsInput = state.carbsPercent.toString()
        if (proteinInput.isEmpty()) proteinInput = state.proteinPercent.toString()
        if (fatInput.isEmpty()) fatInput = state.fatPercent.toString()
    }

    val carbsG = (state.calorieGoal * state.carbsPercent / 100) / 4
    val proteinG = (state.calorieGoal * state.proteinPercent / 100) / 4
    val fatG = (state.calorieGoal * state.fatPercent / 100) / 9

    val sum = (carbsInput.toIntOrNull() ?: 0) + (proteinInput.toIntOrNull() ?: 0) + (fatInput.toIntOrNull() ?: 0)
    val isSumValid = sum == 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Calorie Goal Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Calories Target",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = calorieInput,
                        onValueChange = {
                            calorieInput = it
                            val cal = it.toIntOrNull()
                            if (cal != null && cal > 0) {
                                viewModel.updateCalorieGoal(cal)
                            }
                        },
                        label = { Text("Calories (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = onNavigateToCalculator,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use daily calorie goal calculator", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Macro splits card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Macro Splits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Configure your macronutrient intake percentages. They must sum to exactly 100%. Currently: $sum%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSumValid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else ErrorRed
                    )

                    // Carbohydrates
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Carbohydrates ($carbsG g)", fontWeight = FontWeight.Bold)
                            Text("1g = 4 kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        OutlinedTextField(
                            value = carbsInput,
                            onValueChange = {
                                carbsInput = it
                                val carbsVal = it.toIntOrNull() ?: 0
                                val proteinVal = proteinInput.toIntOrNull() ?: 0
                                val fatVal = fatInput.toIntOrNull() ?: 0
                                viewModel.updateMacroPercentages(carbsVal, proteinVal, fatVal)
                            },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    // Protein
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Protein ($proteinG g)", fontWeight = FontWeight.Bold)
                            Text("1g = 4 kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        OutlinedTextField(
                            value = proteinInput,
                            onValueChange = {
                                proteinInput = it
                                val carbsVal = carbsInput.toIntOrNull() ?: 0
                                val proteinVal = it.toIntOrNull() ?: 0
                                val fatVal = fatInput.toIntOrNull() ?: 0
                                viewModel.updateMacroPercentages(carbsVal, proteinVal, fatVal)
                            },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    // Fat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fat ($fatG g)", fontWeight = FontWeight.Bold)
                            Text("1g = 9 kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        OutlinedTextField(
                            value = fatInput,
                            onValueChange = {
                                fatInput = it
                                val carbsVal = carbsInput.toIntOrNull() ?: 0
                                val proteinVal = proteinInput.toIntOrNull() ?: 0
                                val fatVal = it.toIntOrNull() ?: 0
                                viewModel.updateMacroPercentages(carbsVal, proteinVal, fatVal)
                            },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
        }
    }
}
