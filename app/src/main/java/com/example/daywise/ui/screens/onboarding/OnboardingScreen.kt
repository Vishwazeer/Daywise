package com.example.daywise.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daywise.data.preferences.PreferencesManager

// ═══════════════════════════════════════════════════════════════════════════
// OnboardingScreen – entry point
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: OnboardingViewModel = viewModel {
        OnboardingViewModel(PreferencesManager(context))
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Track previous step for transition direction
    val previousStep = remember { mutableIntStateOf(0) }
    val isForward = state.currentStep >= previousStep.intValue

    Scaffold(
        modifier = modifier,
        topBar = {
            if (state.currentStep > 0) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = {
                            previousStep.intValue = state.currentStep
                            viewModel.previousStep()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Progress bar ────────────────────────────────────────────
            if (state.currentStep > 0) {
                val progress by animateFloatAsState(
                    targetValue = state.currentStep.toFloat() / (state.totalSteps - 1).coerceAtLeast(1).toFloat(),
                    animationSpec = tween(400),
                    label = "progress",
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Animated step content ───────────────────────────────────
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (isForward) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "step_transition",
                modifier = Modifier.weight(1f),
            ) { step ->
                when (step) {
                    0 -> WelcomeStep()
                    1 -> BasicInfoStep(state, viewModel)
                    2 -> GoalSelectionStep(state, viewModel)
                    3 -> AggressionStep(state, viewModel)
                    4 -> TargetWeightStep(state, viewModel)
                    5 -> ResultsStep(state)
                }
            }

            // ── Bottom action button ────────────────────────────────────
            val isLastStep = (state.goal == WeightGoal.Maintain && state.currentStep == 4) ||
                    state.currentStep == 5

            Button(
                onClick = {
                    previousStep.intValue = state.currentStep
                    if (isLastStep) {
                        viewModel.completeOnboarding(onComplete = onFinished)
                    } else {
                        viewModel.nextStep()
                    }
                },
                enabled = state.canAdvance && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = when {
                            state.currentStep == 0 -> "Get Started"
                            isLastStep -> "Start Tracking"
                            else -> "Continue"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 0 – Welcome
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Spa,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Welcome to Daywise",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Let's set up your health goals",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 1 – Basic Info
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BasicInfoStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "This helps us calculate your personalized plan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        // Gender selection
        Text(
            text = "Gender",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Gender.entries.forEach { gender ->
                SelectableCard(
                    label = gender.label,
                    selected = state.gender == gender,
                    onClick = { viewModel.setGender(gender) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Age
        OutlinedTextField(
            value = state.ageText,
            onValueChange = viewModel::setAge,
            label = { Text("Age") },
            suffix = { Text("years") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // Height
        OutlinedTextField(
            value = state.heightText,
            onValueChange = viewModel::setHeight,
            label = { Text("Height") },
            suffix = { Text("cm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // Weight
        OutlinedTextField(
            value = state.weightText,
            onValueChange = viewModel::setWeight,
            label = { Text("Current Weight") },
            suffix = { Text("kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 2 – Goal Selection
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GoalSelectionStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "What's your goal?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "We'll tailor your calorie target accordingly",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        WeightGoal.entries.forEach { goal ->
            SelectableCard(
                label = "${goal.emoji}  ${goal.label}",
                selected = state.goal == goal,
                onClick = { viewModel.setGoal(goal) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 3 – Aggression Level
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AggressionStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "How fast do you want to reach your goal?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "A slower pace is more sustainable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        AggressionLevel.entries.forEach { level ->
            SelectableCard(
                label = level.label,
                subtitle = "${level.kgPerWeek} kg / week",
                selected = state.aggression == level,
                onClick = { viewModel.setAggression(level) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 4 – Target Weight
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TargetWeightStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Set your target weight",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(24.dp))

        // Current weight display
        InfoRow(label = "Current weight", value = if (state.weight > 0f) "${"%.1f".format(state.weight)} kg" else "—")
        Spacer(Modifier.height(16.dp))

        // Target weight input
        OutlinedTextField(
            value = state.targetWeightText,
            onValueChange = viewModel::setTargetWeight,
            label = { Text("Target Weight") },
            suffix = { Text("kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        // BMI display card
        if (state.bmi > 0f) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BMI Overview",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(12.dp))

                    val animatedBmi by animateFloatAsState(
                        targetValue = state.bmi,
                        animationSpec = tween(600),
                        label = "bmi_anim",
                    )
                    InfoRow(
                        label = "Current BMI",
                        value = "${"%.1f".format(animatedBmi)} · ${state.bmiCategory.label}",
                    )

                    if (state.isTargetWeightValid && state.targetWeight != state.weight) {
                        Spacer(Modifier.height(8.dp))
                        val animatedTargetBmi by animateFloatAsState(
                            targetValue = state.targetBmi,
                            animationSpec = tween(600),
                            label = "target_bmi_anim",
                        )
                        InfoRow(
                            label = "Target BMI",
                            value = "${"%.1f".format(animatedTargetBmi)} · ${state.targetBmiCategory.label}",
                        )
                    }

                    if (state.estimatedWeeks > 0) {
                        Spacer(Modifier.height(8.dp))
                        val animatedWeeks by animateIntAsState(
                            targetValue = state.estimatedWeeks,
                            animationSpec = tween(600),
                            label = "weeks_anim",
                        )
                        InfoRow(
                            label = "Estimated time",
                            value = "$animatedWeeks weeks",
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 5 – Results & Confirmation
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultsStep(state: OnboardingUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your personalized plan",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Based on your profile and goals",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        // ── Calorie card ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                val animatedCal by animateIntAsState(
                    targetValue = state.dailyCalorieTarget,
                    animationSpec = tween(800),
                    label = "cal_anim",
                )
                Text(
                    text = "$animatedCal",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "calories / day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Macro breakdown ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Macros",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(12.dp))

                val animatedProtein by animateIntAsState(state.proteinGrams, tween(700), label = "p")
                val animatedCarbs by animateIntAsState(state.carbsGrams, tween(700), label = "c")
                val animatedFat by animateIntAsState(state.fatGrams, tween(700), label = "f")

                MacroRow(emoji = "🥩", label = "Protein", grams = animatedProtein, percent = 25)
                Spacer(Modifier.height(8.dp))
                MacroRow(emoji = "🍞", label = "Carbs", grams = animatedCarbs, percent = 50)
                Spacer(Modifier.height(8.dp))
                MacroRow(emoji = "🥑", label = "Fat", grams = animatedFat, percent = 25)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Calculation details ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How we calculated this",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(12.dp))

                val animatedBmr by animateIntAsState(state.bmr, tween(700), label = "bmr")
                val animatedTdee by animateIntAsState(state.tdee, tween(700), label = "tdee")

                InfoRow(label = "BMR (Mifflin-St Jeor)", value = "$animatedBmr cal")
                Spacer(Modifier.height(6.dp))
                InfoRow(label = "TDEE (× 1.3)", value = "$animatedTdee cal")

                if (state.goal != WeightGoal.Maintain && state.aggression != null) {
                    Spacer(Modifier.height(6.dp))
                    val sign = if (state.goal == WeightGoal.Lose) "−" else "+"
                    InfoRow(
                        label = "Adjustment (${state.aggression!!.kgPerWeek} kg/wk)",
                        value = "$sign${state.aggression!!.dailyCalorieAdjustment} cal",
                    )
                }

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                InfoRow(
                    label = "Daily target",
                    value = "${state.dailyCalorieTarget} cal",
                    bold = true,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared UI components
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SelectableCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 2.dp else 0.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    bold: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MacroRow(
    emoji: String,
    label: String,
    grams: Int,
    percent: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${grams}g",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
