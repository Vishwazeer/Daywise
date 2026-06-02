package com.example.daywise.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daywise.data.local.DaywiseDatabase
import com.example.daywise.data.local.entity.ChatMessageEntity
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.data.repository.DaywiseRepository
import com.example.daywise.data.remote.GeminiService
import com.example.daywise.ui.components.*
import com.example.daywise.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToDrawer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val db = remember { DaywiseDatabase.getInstance(context) }
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val geminiKey by prefs.geminiApiKey.collectAsState(initial = "")
    val repository = remember(geminiKey) {
        val gemini = GeminiService(geminiKey)
        DaywiseRepository(
            db.foodDao(),
            db.exerciseDao(),
            db.weightDao(),
            db.chatMessageDao(),
            prefs,
            gemini
        )
    }

    val viewModel: ChatViewModel = viewModel(key = geminiKey) {
        ChatViewModel(repository)
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val nutritionSummary by viewModel.nutritionSummary.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // Load goals preferences for stats
    val calorieGoal by prefs.dailyCalorieGoal.collectAsState(initial = 2000)
    val carbsPercent by prefs.carbsPercent.collectAsState(initial = 50)
    val proteinPercent by prefs.proteinPercent.collectAsState(initial = 25)
    val fatPercent by prefs.fatPercent.collectAsState(initial = 25)

    val proteinGoal = (calorieGoal * proteinPercent / 100) / 4f
    val carbsGoal = (calorieGoal * carbsPercent / 100) / 4f
    val fatGoal = (calorieGoal * fatPercent / 100) / 9f

    val overviewData = DailyOverviewData(
        caloriesConsumed = nutritionSummary.totalCalories,
        calorieGoal = calorieGoal,
        proteinCurrent = nutritionSummary.totalProtein,
        proteinGoal = proteinGoal,
        carbsCurrent = nutritionSummary.totalCarbs,
        carbsGoal = carbsGoal,
        fatCurrent = nutritionSummary.totalFat,
        fatGoal = fatGoal,
        exerciseCalories = nutritionSummary.exerciseCalories
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    selectedScreen = "",
                    onNavigate = { screen ->
                        scope.launch { drawerState.close() }
                        onNavigateToDrawer(screen)
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Daywise", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                // Calendar Strip (scrollable dates)
                CalendarStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.onDateSelected(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Stats Dashboard Overview Card
                DailyOverviewCard(
                    data = overviewData,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Chat Messages LazyColumn
                val listState = rememberLazyListState()
                LaunchedEffect(chatMessages.size) {
                    if (chatMessages.isNotEmpty()) {
                        listState.animateScrollToItem(chatMessages.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatMessages) { message ->
                            ChatBubble(message)
                        }

                        if (isProcessing) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat input text bar at bottom
                var textInput by remember { mutableStateOf("") }
                
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Log your food or exercise...") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            })
                        )
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (message.isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
