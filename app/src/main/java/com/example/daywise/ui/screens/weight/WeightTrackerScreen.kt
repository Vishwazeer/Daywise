package com.example.daywise.ui.screens.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daywise.data.local.DaywiseDatabase
import com.example.daywise.data.local.entity.WeightEntryEntity
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.data.repository.DaywiseRepository
import com.example.daywise.data.remote.GeminiService
import com.example.daywise.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val db = DaywiseDatabase.getInstance(context)
    val prefs = PreferencesManager(context)
    val gemini = GeminiService("")
    val repository = DaywiseRepository(
        db.foodDao(),
        db.exerciseDao(),
        db.weightDao(),
        db.chatMessageDao(),
        prefs,
        gemini
    )

    val viewModel: WeightViewModel = viewModel {
        WeightViewModel(repository, prefs)
    }

    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Entry")
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
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Current Weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.currentWeightKg} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Target Weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.targetWeightKg} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Time Period Tabs
            TabRow(
                selectedTabIndex = state.selectedPeriod.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {}
            ) {
                TimePeriod.values().forEach { period ->
                    Tab(
                        selected = state.selectedPeriod == period,
                        onClick = { viewModel.selectPeriod(period) },
                        text = {
                            Text(
                                text = when (period) {
                                    TimePeriod.WEEK -> "Week"
                                    TimePeriod.MONTH -> "Month"
                                    TimePeriod.YEAR -> "Year"
                                    TimePeriod.ALL -> "All time"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (state.filteredEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No weight entries recorded yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    WeightChart(
                        entries = state.filteredEntries,
                        targetWeight = state.targetWeightKg,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        accentColor = SuccessGreen
                    )
                }
            }

            // Weight Entries list title
            Text(
                text = "Weight Entries",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Weight Entries list (descending order)
            val listEntries = remember(state.entries) { state.entries }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listEntries, key = { it.id }) { entry ->
                    val dateFormat = SimpleDateFormat("EEEE, d MMM, yyyy h:mm a", Locale.getDefault())
                    val dateStr = dateFormat.format(Date(entry.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${entry.weightKg} kg",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Entry Dialog
    if (showAddDialog) {
        var weightInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Weight", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightInput.toFloatOrNull()
                        if (w != null && w > 0) {
                            viewModel.addWeight(w)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WeightChart(
    entries: List<WeightEntryEntity>,
    targetWeight: Float,
    primaryColor: Color,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Calculate Y scale bounds
        val weightValues = entries.map { it.weightKg } + targetWeight
        val maxWeight = weightValues.maxOrNull() ?: 100f
        val minWeight = weightValues.minOrNull() ?: 50f
        val padding = (maxWeight - minWeight).coerceAtLeast(5f) * 0.15f
        
        val yMax = maxWeight + padding
        val yMin = (minWeight - padding).coerceAtLeast(0f)
        val yRange = yMax - yMin

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * i / gridLines
            val weightLabel = yMax - (yRange * i / gridLines)
            // Draw grid line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(40.dp.toPx(), y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw target weight dashed line
        val targetY = height - ((targetWeight - yMin) / yRange * height)
        drawLine(
            color = accentColor,
            start = Offset(40.dp.toPx(), targetY),
            end = Offset(width, targetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Draw weight entry data points and connection lines
        if (entries.isNotEmpty()) {
            val points = entries.mapIndexed { index, entry ->
                val x = if (entries.size > 1) {
                    40.dp.toPx() + (width - 40.dp.toPx()) * index / (entries.size - 1)
                } else {
                    width / 2
                }
                val y = height - ((entry.weightKg - yMin) / yRange * height)
                Offset(x, y)
            }

            // Draw line connecting points
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Draw points as circles
            points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = point
                )
            }
        }
    }
}
