package com.martinosama.habittracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.martinosama.habittracker.backend.AppwriteClient
import com.martinosama.habittracker.ui.theme.HabitTrackerTheme
import com.martinosama.habittracker.ui.theme.PrimaryGreen
import com.martinosama.habittracker.ui.theme.SecondaryGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.martinosama.habittracker.backend.HabitRepository
import com.martinosama.habittracker.backend.ReminderRepository
import com.martinosama.habittracker.backend.RetrofitInstance
import com.martinosama.habittracker.model.HabitDto
import com.martinosama.habittracker.model.ReminderDto
import com.martinosama.habittracker.ui.theme.Gray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Habit(
    val id: String = "",
    val name: String = "",
    val notes: String = "",
    val date: String = "",
    val time: String = "",
    val isCompleted: Boolean = false,
    val remindBeforeMinutes: Int = 0,
    val reminders: List<ReminderDto> = emptyList()
)

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val habitRepository = HabitRepository(RetrofitInstance.habitApi)
    private val reminderRepository = ReminderRepository(RetrofitInstance.reminderApi)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openBatteryOptimizationSettings(this)
        requestExactAlarmPermission(this)

        AppwriteClient.init(this)

        setContent {
            HabitTrackerTheme {
                val context = LocalContext.current
                var selectedTab by remember { mutableIntStateOf(0) }
                var showAddDialog by remember { mutableStateOf(false) }
                var userName by remember { mutableStateOf("User") }
                var avatarUrl by remember { mutableStateOf("") }
                var habits by remember { mutableStateOf(listOf<Habit>()) }
                val db = FirebaseFirestore.getInstance()

                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userDoc = db.collection("users").document(user.uid).get().await()
                        val profile = userDoc.toObject(UserProfile::class.java)
                        userName = profile?.firstName?.takeIf { it.isNotBlank() } ?: "User"
                        avatarUrl = profile?.avatarUrl ?: ""
                        try {
                            val habitDtos = habitRepository.getHabits(user.uid)
                            habits = habitDtos.map { dto ->
                                val reminders = reminderRepository.getReminders(dto.id!!)
                                Habit(
                                    id = dto.id,
                                    name = dto.title,
                                    notes = dto.notes.orEmpty(),
                                    date = dto.date,
                                    time = "${dto.fromTime} – ${dto.toTime}",
                                    isCompleted = dto.isCompleted,
                                    remindBeforeMinutes = when (dto.reminder) {
                                        "5 min" -> 5
                                        "10 min" -> 10
                                        "30 min" -> 30
                                        "1 hour" -> 60
                                        else -> 0
                                    },
                                    reminders = reminders
                                )
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Failed to load habits: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                var showNotificationDialog by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                    )
                }

                if (showNotificationDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    RequestNotificationPermissionDialog(
                        onAllow = {
                            showNotificationDialog = false
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onDismiss = { showNotificationDialog = false }
                    )
                }

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(selectedTab = selectedTab) {
                            selectedTab = it
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = selectedTab == 0,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                containerColor = SecondaryGreen,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Habit")
                            }
                        }
                    },
                    containerColor = Color.White
                ) { paddingValues ->
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith
                                        slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "TabAnimation"
                    ) { tab ->
                        when (tab) {
                            0 -> MainPage(
                                modifier = Modifier,
                                habits = habits,
                                userName = userName,
                                avatarUrl = avatarUrl,
                                showAddDialog = showAddDialog,
                                onShowAddDialogChange = { showAddDialog = it },
                                onAddHabit = { newHabit ->
                                    val user = auth.currentUser
                                    if (user != null) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val habitDto = HabitDto(
                                                    title = newHabit.name,
                                                    notes = newHabit.notes,
                                                    date = newHabit.date,
                                                    fromTime = newHabit.time.split("–")[0].trim(),
                                                    toTime = newHabit.time.split("–")[1].trim(),
                                                    reminder = when (newHabit.remindBeforeMinutes) {
                                                        5 -> "5 min"
                                                        10 -> "10 min"
                                                        30 -> "30 min"
                                                        60 -> "1 hour"
                                                        else -> null
                                                    },
                                                    isCompleted = newHabit.isCompleted
                                                )
                                                val createdHabit = habitRepository.createHabit(user.uid, habitDto)

                                                if (newHabit.remindBeforeMinutes > 0) {
                                                    val remindTime = calculateReminderTime(newHabit)
                                                    reminderRepository.createReminder(
                                                        ReminderDto(
                                                            habitId = createdHabit.id!!,
                                                            remindAt = remindTime
                                                        )
                                                    )
                                                }

                                                withContext(Dispatchers.Main) {
                                                    habits = habits + newHabit.copy(id = createdHabit.id!!)
                                                    scheduleHabitReminder(this@MainActivity, newHabit)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(this@MainActivity, "Failed to save habit: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                onHabitCheckChanged = { habitId, isCompleted ->
                                    val habitToUpdate = habits.find { it.id == habitId }

                                    if (habitToUpdate != null) {
                                        val updatedHabitDto = HabitDto(
                                            title = habitToUpdate.name,
                                            notes = habitToUpdate.notes,
                                            date = habitToUpdate.date,
                                            fromTime = habitToUpdate.time.split("–")[0].trim(),
                                            toTime = habitToUpdate.time.split("–")[1].trim(),
                                            reminder = when (habitToUpdate.remindBeforeMinutes) {
                                                5 -> "5 min"
                                                10 -> "10 min"
                                                30 -> "30 min"
                                                60 -> "1 hour"
                                                else -> null
                                            },
                                            isCompleted = isCompleted
                                        )

                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val success = habitRepository.updateHabit(habitId, updatedHabitDto)
                                                withContext(Dispatchers.Main) {
                                                    if (success) {
                                                        habits = habits.map { habit ->
                                                            if (habit.id == habitId) habit.copy(isCompleted = isCompleted) else habit
                                                        }
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "Failed to update habit", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Habit not found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onHabitDelete = { habitId ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val reminders = reminderRepository.getReminders(habitId)
                                            reminders.forEach { reminder ->
                                                reminderRepository.deleteReminder(reminder.id)
                                            }
                                            val success = habitRepository.deleteHabit(habitId)
                                            withContext(Dispatchers.Main) {
                                                if (success) {
                                                    habits = habits.filter { it.id != habitId }
                                                    Toast.makeText(this@MainActivity, "Habit deleted", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Failed to delete habit", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Error deleting habit: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                onHabitEdit = { updatedHabit ->
                                    val user = auth.currentUser
                                    if (user != null) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val habitDto = HabitDto(
                                                    title = updatedHabit.name,
                                                    notes = updatedHabit.notes,
                                                    date = updatedHabit.date,
                                                    fromTime = updatedHabit.time.split("–")[0].trim(),
                                                    toTime = updatedHabit.time.split("–")[1].trim(),
                                                    reminder = when (updatedHabit.remindBeforeMinutes) {
                                                        5 -> "5 min"
                                                        10 -> "10 min"
                                                        30 -> "30 min"
                                                        60 -> "1 hour"
                                                        else -> null
                                                    },
                                                    isCompleted = updatedHabit.isCompleted
                                                )
                                                val success = habitRepository.updateHabit(updatedHabit.id, habitDto)

                                                withContext(Dispatchers.Main) {
                                                    if (success) {
                                                        habits = habits.map { habit ->
                                                            if (habit.id == updatedHabit.id) updatedHabit else habit
                                                        }
                                                        Toast.makeText(this@MainActivity, "Habit updated", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "Update failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(this@MainActivity, "Error updating habit: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                            1 -> StatisticsPage(
                                modifier = Modifier.padding(paddingValues),
                                habits = habits
                            )
                        }
                    }
                }
            }
        }
    }

    private fun calculateReminderTime(habit: Habit): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateTime = LocalDateTime.parse("${habit.date} ${habit.time.split("–")[0].trim()}", formatter)
        val reminderTime = dateTime.minusMinutes(habit.remindBeforeMinutes.toLong())
        return reminderTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @Composable
    fun RequestNotificationPermissionDialog(
        onAllow: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(Icons.Default.Notifications, contentDescription = null)
                   },
            title = {
                Text("Allow HabitTracker to send you notifications?")
                    },
            text = {
                Text("This lets us remind you of upcoming habits.")
                   },
            confirmButton = {
                TextButton(onClick = onAllow) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Don’t allow")
                }
            }
        )
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
                } else {
                    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                    if (shouldShowRationale) {
                        Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Permission permanently denied. Enable it in app settings.",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    }
                }
            }
        }

    private fun openBatteryOptimizationSettings(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            context.startActivity(intent)
        }
    }

    private fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }
        }
    }

    @Composable
    fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.EventAvailable, contentDescription = "Habits") },
                label = { Text("Habits") },
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = SecondaryGreen,
                    selectedTextColor = SecondaryGreen
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.DataUsage, contentDescription = "Statistics") },
                label = { Text("Stats") },
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = SecondaryGreen,
                    selectedTextColor = SecondaryGreen
                )
            )
        }
    }

    @Composable
    fun StatisticsPage(
        modifier: Modifier = Modifier,
        habits: List<Habit>
    ) {
        var selectedMonth by remember {
            mutableStateOf(LocalDate.now().withDayOfMonth(1))
        }

        val currentMonth = remember(selectedMonth) {
            selectedMonth.format(DateTimeFormatter.ofPattern("MMMM"))
        }

        val today = LocalDate.now().withDayOfMonth(1)
        val isNextEnabled = selectedMonth < today

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineSmall.copy(color = PrimaryGreen)
            )

            Spacer(modifier = Modifier.height(24.dp))

            MonthlyPieChart(
                currentMonth = currentMonth,
                selectedMonthDate = selectedMonth,
                habits = habits,
                onPreviousMonth = {
                    selectedMonth = selectedMonth.minusMonths(1)
                },
                onNextMonth = {
                    if (isNextEnabled) selectedMonth = selectedMonth.plusMonths(1)
                },
                isNextEnabled = isNextEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Monthly Habit Log",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            )

            val monthHabits = remember(habits, selectedMonth) {
                habits.mapNotNull { habit ->
                    runCatching {
                        val habitDate = LocalDate.parse(habit.date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        Pair(habitDate, habit)
                    }.getOrNull()
                }.filter { (date, _) ->
                    date.year == selectedMonth.year && date.month == selectedMonth.month
                }.sortedWith(compareBy({ it.first }, { it.second.time }))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(monthHabits) { (habitDate, habit) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "${habitDate.format(DateTimeFormatter.ofPattern("dd MMM"))} • ${habit.time}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (habit.notes.isNotBlank()) {
                                    Text(
                                        text = habit.notes,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.DarkGray)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (habit.isCompleted) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (habit.isCompleted) PrimaryGreen else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MonthlyPieChart(
        modifier: Modifier = Modifier,
        currentMonth: String,
        selectedMonthDate: LocalDate,
        habits: List<Habit>,
        onPreviousMonth: () -> Unit,
        onNextMonth: () -> Unit,
        isNextEnabled: Boolean
    ) {
        val context = LocalContext.current

        val filteredHabits = remember(habits, selectedMonthDate) {
            habits.filter { habit ->
                runCatching {
                    LocalDate.parse(habit.date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }.getOrNull()?.let { date ->
                    date.year == selectedMonthDate.year &&
                            date.month == selectedMonthDate.month
                } ?: false
            }
        }

        val completedCount = filteredHabits.count { it.isCompleted }
        val missedCount = filteredHabits.count { !it.isCompleted }

        val entries = if (filteredHabits.isEmpty()) {
            listOf(PieEntry(1f))
        } else {
            listOf(
                PieEntry(completedCount.toFloat(), "Completed"),
                PieEntry(missedCount.toFloat(),    "Missed")
            )
        }

        Box(
            modifier = modifier.size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    PieChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.isEnabled = false
                        isHighlightPerTapEnabled = false
                        isRotationEnabled = false
                        setTouchEnabled(false)
                        setUsePercentValues(false)
                        setDrawEntryLabels(false)
                        setHoleColor(android.graphics.Color.TRANSPARENT)
                        holeRadius = 70f
                        transparentCircleRadius = 75f
                    }
                },
                update = { chart ->
                    val dataSet = PieDataSet(entries, "").apply {
                        setDrawValues(false)
                        when (entries.size) {
                            1 -> setColors(Gray.toArgb())
                            2 -> setColors(
                                PrimaryGreen.toArgb(),
                                android.graphics.Color.RED
                            )
                        }
                    }
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                }
            )

            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = PrimaryGreen
                    )
                }
                Text(
                    text  = currentMonth,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = PrimaryGreen,
                        fontSize = 18.sp
                    )
                )
                IconButton(onClick = onNextMonth, enabled = isNextEnabled) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = if (isNextEnabled) PrimaryGreen else Color.Gray
                    )
                }
            }
        }
    }

    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainPage(
        userName: String,
        avatarUrl: String,
        modifier: Modifier = Modifier,
        habits: List<Habit>,
        showAddDialog: Boolean,
        onShowAddDialogChange: (Boolean) -> Unit,
        onAddHabit: (Habit) -> Unit,
        onHabitCheckChanged: (String, Boolean) -> Unit,
        onHabitDelete: (String) -> Unit,
        onHabitEdit: (Habit) -> Unit
    ) {
        val context = LocalContext.current
        val greetingMessage = getGreetingMessage()
        var editingHabit by remember { mutableStateOf<Habit?>(null) }
        var showEditDialog by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }
        val defaultAvatarUrl =
            "https://cloud.appwrite.io/v1/storage/buckets/67c5b7e7003987ac953d/files/67c73807003c74d72dd0/view?project=67c5b7900039311024c6&mode=admin"

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "$greetingMessage, $userName!",
                                modifier = Modifier
                                    .padding(start = 16.dp)
                            )
                        },
                        actions = {
                            val displayUrl = avatarUrl.ifEmpty { defaultAvatarUrl }
                            IconButton(
                                modifier = Modifier
                                    .padding(end = 16.dp),
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            ProfileActivity::class.java
                                        )
                                    )
                                }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(displayUrl),
                                    contentDescription = "Profile Avatar",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    )
                    HorizontalDivider(
                        color = Color(0xFFE0E0E0),
                        thickness = 3.dp
                    )
                }
            },
            content = { padding ->
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding(),
                            start = 16.dp,
                            end = 16.dp
                        )
                        .background(Color.White),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    HabitCalendarHeader(
                        selectedDate = selectedDate,
                        onDateChange = { selectedDate = it }
                    )
                    val habitsForSelectedDate = habits.filter {
                        parseDate(it.date) == selectedDate
                    }.sortedBy { habit ->
                        habit.time.split("–").firstOrNull()?.trim()
                            ?.takeIf { time -> time.matches(Regex("\\d{2}:\\d{2}")) } ?: "00:00"
                    }
                    val completedForSelected = habitsForSelectedDate.count { it.isCompleted }
                    Spacer(modifier = Modifier.height(5.dp))
                    ProgressSection(
                        completed = completedForSelected,
                        total = habitsForSelectedDate.size
                    )
                    HabitList(
                        habits = habitsForSelectedDate,
                        onHabitCheckChanged = { habitId, isCompleted ->
                            onHabitCheckChanged(habitId, isCompleted)
                        },
                        onHabitDelete = onHabitDelete,
                        onHabitEdit = { habit ->
                            editingHabit = habit
                            showEditDialog = true
                        },
                        contentPadding = padding
                    )
                    if (showAddDialog) {
                        AddHabitDialog(
                            onDismiss = { onShowAddDialogChange(false) },
                            onSave = { newHabit ->
                                onAddHabit(newHabit)
                                onShowAddDialogChange(false)
                            }
                        )
                    }

                    if (showEditDialog && editingHabit != null) {
                        EditHabitDialog(
                            habit = editingHabit!!,
                            onDismiss = { showEditDialog = false },
                            onSave = { updatedHabit ->
                                onHabitEdit(updatedHabit)
                                showEditDialog = false
                            }
                        )
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddHabitDialog(
        onDismiss: () -> Unit,
        onSave: (Habit) -> Unit
    ) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var date by remember { mutableStateOf("") }
        val dateFormatter = remember {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                isLenient = false
            }
        }
        var timeFrom by remember { mutableStateOf("") }
        var timeTo by remember { mutableStateOf("") }
        var showComposeFromPicker by remember { mutableStateOf(false) }
        var showComposeToPicker by remember { mutableStateOf(false) }
        var earlyReminder by remember { mutableStateOf(false) }
        val reminderOptions = listOf("5 min", "10 min", "30 min", "1 hour")
        var selectedReminder by remember { mutableStateOf(reminderOptions[0]) }
        var expanded by remember { mutableStateOf(false) }
        val noRipples = remember { MutableInteractionSource() }
        var showDatePicker by remember { mutableStateOf(false) }

        if (showComposeFromPicker) {
            Dialog(onDismissRequest = { showComposeFromPicker = false }) {
                val state = rememberTimePickerState(
                    initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
                    is24Hour = true
                )
                var showKeyboardInputFrom by remember { mutableStateOf(false) }
                var hourInput by remember { mutableStateOf("") }
                var minuteInput by remember { mutableStateOf("") }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Select start time", style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = {
                                showKeyboardInputFrom = !showKeyboardInputFrom
                            }) {
                                Icon(
                                    imageVector = if (showKeyboardInputFrom) Icons.Default.AccessTime else Icons.Default.Keyboard,
                                    contentDescription = "Toggle Input Mode"
                                )
                            }
                        }

                        if (showKeyboardInputFrom) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = hourInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        val value = digits.toIntOrNull()
                                        if (digits.isEmpty()) {
                                            hourInput = ""
                                        } else if (value != null && value in 0..23) {
                                            hourInput = digits
                                        }
                                    },
                                    label = { Text("Hour") },
                                    modifier = Modifier
                                        .weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = minuteInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        val value = digits.toIntOrNull()
                                        if (digits.isEmpty()) {
                                            minuteInput = ""
                                        } else if (value != null && value in 0..59) {
                                            minuteInput = digits
                                        }
                                    },
                                    label = { Text("Minute") },
                                    modifier = Modifier
                                        .weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        } else {
                            TimePicker(
                                state = state,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            TextButton(onClick = { showComposeFromPicker = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                timeFrom = if (showKeyboardInputFrom) {
                                    val hour = hourInput.toIntOrNull() ?: 0
                                    val minute = minuteInput.toIntOrNull() ?: 0
                                    "%02d:%02d".format(hour, minute)
                                } else {
                                    "%02d:%02d".format(state.hour, state.minute)
                                }
                                showComposeFromPicker = false
                            }) {
                                Text("OK", color = PrimaryGreen)
                            }
                        }
                    }
                }
            }
        }

        if (showComposeToPicker) {
            Dialog(onDismissRequest = { showComposeToPicker = false }) {
                val state = rememberTimePickerState(
                    initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
                    is24Hour = true
                )
                var showKeyboardInputTo by remember { mutableStateOf(false) }
                var hourInput by remember { mutableStateOf("") }
                var minuteInput by remember { mutableStateOf("") }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Select end time", style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { showKeyboardInputTo = !showKeyboardInputTo }) {
                                Icon(
                                    imageVector = if (showKeyboardInputTo) Icons.Default.AccessTime else Icons.Default.Keyboard,
                                    contentDescription = "Toggle Input Mode"
                                )
                            }
                        }

                        if (showKeyboardInputTo) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = hourInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        val value = digits.toIntOrNull()
                                        if (digits.isEmpty()) {
                                            hourInput = ""
                                        } else if (value != null && value in 0..23) {
                                            hourInput = digits
                                        }
                                    },
                                    label = { Text("Hour") },
                                    modifier = Modifier
                                        .weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = minuteInput,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        val value = digits.toIntOrNull()
                                        if (digits.isEmpty()) {
                                            minuteInput = ""
                                        } else if (value != null && value in 0..59) {
                                            minuteInput = digits
                                        }
                                    },
                                    label = { Text("Minute") },
                                    modifier = Modifier
                                        .weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        } else {
                            TimePicker(
                                state = state,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            TextButton(onClick = { showComposeToPicker = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                timeTo = if (showKeyboardInputTo) {
                                    val hour = hourInput.toIntOrNull() ?: 0
                                    val minute = minuteInput.toIntOrNull() ?: 0
                                    "%02d:%02d".format(hour, minute)
                                } else {
                                    "%02d:%02d".format(state.hour, state.minute)
                                }
                                showComposeToPicker = false
                            }) {
                                Text("OK", color = PrimaryGreen)
                            }
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val selectedDate = Date(millis)
                            date = dateFormatter.format(selectedDate)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK", color = PrimaryGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("New Habit", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier
                            .fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        placeholder = { Text("dd/MM/yyyy") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = noRipples,
                                indication = null
                            ) { showDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = Color.Transparent
                        ),
                        enabled = false,
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timeFrom,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("From") },
                            placeholder = { Text("Select time") },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(
                                    interactionSource = noRipples,
                                    indication = null
                                ) { showComposeFromPicker = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = Color.Transparent
                            )
                        )

                        OutlinedTextField(
                            value = timeTo,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("To") },
                            placeholder = { Text("Select time") },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(
                                    interactionSource = noRipples,
                                    indication = null
                                ) { showComposeToPicker = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = Color.Transparent
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text("Early Reminder")
                            Switch(
                                checked = earlyReminder,
                                onCheckedChange = { earlyReminder = it }
                            )
                        }

                        if (earlyReminder) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = selectedReminder,
                                    onValueChange = {},
                                    label = { Text("Remind me before") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    reminderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedReminder = option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val minutesBefore = when (selectedReminder) {
                                    "5 min"  -> 5
                                    "10 min" -> 10
                                    "30 min" -> 30
                                    "1 hour" -> 60
                                    else     -> 0
                                }

                                onSave(
                                    Habit(
                                        id = UUID.randomUUID().toString(),
                                        name = title,
                                        notes = notes,
                                        date = date,
                                        time = "$timeFrom – $timeTo",
                                        isCompleted = false,
                                        remindBeforeMinutes = if (earlyReminder) minutesBefore else 0
                                    )
                                )
                            },
                            enabled = title.isNotBlank() && date.isNotBlank() && timeFrom.isNotBlank() && timeTo.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    private fun scheduleHabitReminder(context: Context, habit: Habit) {
        if (habit.remindBeforeMinutes <= 0) return

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateTime = LocalDateTime.parse(
            "${habit.date} ${habit.time.split("–")[0].trim()}",
            dateFormatter
        )

        val reminderTime = dateTime.minusMinutes(habit.remindBeforeMinutes.toLong())
        if (reminderTime.isBefore(LocalDateTime.now())) return
        val millis = reminderTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("habitId", habit.id)
            putExtra("habitName", habit.name)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            millis,
            pending
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HabitCalendarHeader(
        selectedDate: LocalDate,
        onDateChange: (LocalDate) -> Unit
    ) {
        var showDatePicker by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = MaterialTheme.typography.titleMedium.copy(color = SecondaryGreen)
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli(),
                yearRange = 2000..2100
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateChange(newDate)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK", color = PrimaryGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    private fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            LocalDate.now()
        }
    }


    @Composable
    fun ProgressSection(completed: Int, total: Int) {
        val targetProgress = if (total > 0) completed.toFloat() / total else 0f
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            label = "progressAnim"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "$completed of $total habits completed",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.LightGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(50))
                        .background(PrimaryGreen)
                )
            }
        }
    }


    @Composable
    fun HabitList(
        habits: List<Habit>,
        onHabitCheckChanged: (String, Boolean) -> Unit,
        onHabitDelete: (String) -> Unit,
        onHabitEdit: (Habit) -> Unit,
        contentPadding: PaddingValues = PaddingValues()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 75.dp
            )
        ) {
            items(habits) { habit ->
                HabitItem(
                    habit = habit,
                    onHabitCheckChanged = { isCompleted ->
                        onHabitCheckChanged(habit.id, isCompleted)
                    },
                    onHabitDelete = { onHabitDelete(habit.id) },
                    onHabitEdit = { onHabitEdit(habit) }
                )
            }
        }
    }

    @Composable
    fun HabitItem(
        habit: Habit,
        onHabitCheckChanged: (Boolean) -> Unit,
        onHabitDelete: () -> Unit,
        onHabitEdit: () -> Unit
    ) {
        var showOptionsDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = habit.isCompleted,
                    onCheckedChange = { newState ->
                        onHabitCheckChanged(newState)
                    }
                )
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                ) {
                    Text(habit.name, fontSize = 18.sp)
                    if (habit.notes.isNotBlank()) {
                        Text(habit.notes, fontSize = 14.sp, color = Color.DarkGray)
                    }
                    Text("Scheduled Time: ${habit.time}", fontSize = 14.sp, color = Color.Gray)
                }
                IconButton(onClick = { showOptionsDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Habit Info",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(28.dp)
                    )
                }
            }
        }

        if (showOptionsDialog) {
            Dialog(
                onDismissRequest = { showOptionsDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xF0F5FDF6),
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "What would you like to do?",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF555555),
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                onHabitDelete()
                                showOptionsDialog = false
                            }) {
                                Text("Delete", color = Color.Red)
                            }
                            TextButton(onClick = {
                                onHabitEdit()
                                showOptionsDialog = false
                            }) {
                                Text("Edit", color = Color(0xFF4CAF50))
                            }
                            TextButton(onClick = {
                                showOptionsDialog = false
                            }) {
                                Text("Cancel", color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EditHabitDialog(
        habit: Habit,
        onDismiss: () -> Unit,
        onSave: (Habit) -> Unit
    ) {
        var title by remember { mutableStateOf(habit.name) }
        var notes by remember { mutableStateOf(habit.notes) }

        Dialog(onDismissRequest = { onDismiss() }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Edit Habit", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { onDismiss() }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onSave(
                                    habit.copy(
                                        name = title,
                                        notes = notes
                                    )
                                )
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}