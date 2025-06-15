package com.martinosama.habittracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.martinosama.habittracker.backend.AppwriteClient
import com.martinosama.habittracker.ui.theme.HabitTrackerTheme
import com.google.firebase.auth.FirebaseAuth
import com.martinosama.habittracker.backend.RetrofitInstance
import com.martinosama.habittracker.backend.UserRepository
import com.martinosama.habittracker.model.UserDto
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val avatarUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
class ProfileActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userRepository = UserRepository(RetrofitInstance.userApi)

        setContent {
            HabitTrackerTheme {
                ProfilePage(
                    auth = auth,
                    userRepository = userRepository,
                    onBack = { finish() }
                )
            }
        }
    }

    @Composable
    fun ProfilePage(
        auth: FirebaseAuth,
        userRepository: UserRepository,
        onBack: () -> Unit
    ) {
        val user = auth.currentUser
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var selectedDay by remember { mutableStateOf("") }
        var selectedMonth by remember { mutableStateOf("") }
        var selectedYear by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("") }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = 1900
        val maxYear = currentYear - 12
        val yearOptions = (maxYear downTo minYear).map { it.toString() }
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val genderOptions = listOf("Male", "Female")
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val defaultAvatarUrl = "https://cloud.appwrite.io/v1/storage/buckets/67c5b7e7003987ac953d/files/67c73807003c74d72dd0/view?project=67c5b7900039311024c6&mode=admin"
        var avatarUrl by remember { mutableStateOf("") }
        var showAvatarOptionsDialog by remember { mutableStateOf(false) }
        val pickImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val uploadedUrl = uploadAvatarToAppwrite(context, uri)
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                    if (uploadedUrl != null) {
                        val updateSuccess = userRepository.updateUser(
                            uid,
                            UserDto(
                                firstName = firstName,
                                lastName = lastName,
                                birthday = String.format(
                                    Locale.US,
                                    "%02d/%02d/%04d",
                                    selectedDay.toIntOrNull() ?: 1,
                                    months.indexOf(selectedMonth) + 1,
                                    selectedYear.toIntOrNull() ?: 1970
                                ),
                                gender = gender,
                                profileImageUrl = uploadedUrl
                            )
                        )
                        if (updateSuccess) {
                            avatarUrl = uploadedUrl
                            Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update avatar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            user?.uid?.let { uid ->
                val userDto = userRepository.getUser(uid)
                userDto?.let { profile ->
                    firstName = profile.firstName
                    lastName = profile.lastName
                    avatarUrl = profile.profileImageUrl ?: defaultAvatarUrl

                    if (profile.birthday.isNotEmpty()) {
                        val parts = profile.birthday.split("/")
                        if (parts.size == 3) {
                            selectedDay = parts[0]
                            val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: 0
                            if (monthIndex in months.indices) {
                                selectedMonth = months[monthIndex]
                            }
                            selectedYear = parts[2]
                        }
                    }
                    gender = profile.gender
                }
            }
        }

        fun deleteProfilePhoto() {
            coroutineScope.launch {
                try {
                    AppwriteClient.storage.deleteFile(
                        bucketId = "67c5b7e7003987ac953d",
                        fileId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
                    )
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                    val updateSuccess = userRepository.updateUser(
                        uid,
                        UserDto(
                            firstName = firstName,
                            lastName = lastName,
                            birthday = String.format(Locale.US, "%02d/%02d/%04d", selectedDay.toIntOrNull() ?: 1, months.indexOf(selectedMonth) + 1, selectedYear.toIntOrNull() ?: 1970),
                            gender = gender,
                            profileImageUrl = null
                        )
                    )
                    if (updateSuccess) {
                        avatarUrl = defaultAvatarUrl
                    }
                    avatarUrl = defaultAvatarUrl
                    Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to remove photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun calculateDays(month: String, year: String): List<String> {
            if (month.isEmpty() || year.isEmpty()) return (1..31).map { it.toString() }
            val monthIndex = months.indexOf(month) + 1
            val yearInt = year.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            return when (monthIndex) {
                4, 6, 9, 11 -> (1..30).map { it.toString() }
                2 -> {
                    if (yearInt % 4 == 0 && (yearInt % 100 != 0 || yearInt % 400 == 0)) {
                        (1..29).map { it.toString() }
                    } else {
                        (1..28).map { it.toString() }
                    }
                }
                else -> (1..31).map { it.toString() }
            }
        }

        val days = calculateDays(selectedMonth, selectedYear)

        fun saveProfile() {
            val monthIndex = months.indexOf(selectedMonth) + 1
            val formattedDate = String.format(
                Locale.US,
                "%02d/%02d/%04d",
                selectedDay.toIntOrNull() ?: 1,
                monthIndex,
                selectedYear.toIntOrNull() ?: 1970
            )
            coroutineScope.launch {
                val userDto = UserDto(
                    firstName = firstName,
                    lastName = lastName,
                    birthday = formattedDate,
                    gender = gender,
                    profileImageUrl = if (avatarUrl == defaultAvatarUrl) null else avatarUrl
                )

                val success = userRepository.updateUser(FirebaseAuth.getInstance().currentUser?.uid ?: "", userDto)
                if (success) {
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    (context as? ProfileActivity)?.finish()
                } else {
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Profile") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (avatarUrl == defaultAvatarUrl) {
                                        pickImageLauncher.launch("image/*")
                                    } else {
                                        showAvatarOptionsDialog = true
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (showAvatarOptionsDialog) {
                        AlertDialog(
                            onDismissRequest = { showAvatarOptionsDialog = false },
                            title = { Text("Profile Picture") },
                            text = { Text("What do you want to do?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showAvatarOptionsDialog = false
                                    pickImageLauncher.launch("image/*")
                                }) {
                                    Text("Change Photo")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showAvatarOptionsDialog = false
                                    deleteProfilePhoto()
                                }) {
                                    Text("Delete Photo")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )

                    Text(
                        text = "Birthday",
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .align(Alignment.Start)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedDropdownSelector(
                                label = "Day",
                                options = days,
                                selected = selectedDay,
                                onOptionSelected = { selectedDay = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedDropdownSelector(
                                label = "Month",
                                options = months,
                                selected = selectedMonth,
                                onOptionSelected = { selectedMonth = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedDropdownSelector(
                                label = "Year",
                                options = yearOptions,
                                selected = selectedYear,
                                onOptionSelected = { selectedYear = it }
                            )
                        }
                    }

                    Text(
                        text = "Gender",
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .align(Alignment.Start)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        AnimatedDropdownSelector(
                            label = "Gender",
                            options = genderOptions,
                            selected = gender,
                            onOptionSelected = { gender = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { saveProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save Profile")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            auth.signOut()
                            startActivity(Intent(this@ProfileActivity, SignInActivity::class.java))
                            finish()
                        },
                        modifier = Modifier.wrapContentWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Log Out")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedDropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            readOnly = true,
            singleLine = true,
            maxLines = 1,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

suspend fun uploadAvatarToAppwrite(context: Context, uri: Uri): String? {
    val stream = context.contentResolver.openInputStream(uri)
    val fileBytes = stream?.readBytes() ?: return null

    if (fileBytes.size > 5 * 1024 * 1024) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Image too large (max 5MB)", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    val file = withContext(Dispatchers.IO) {
        File.createTempFile("avatar", ".jpg", context.cacheDir).apply {
            writeBytes(fileBytes)
        }
    }

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    try {
        AppwriteClient.storage.deleteFile(
            bucketId = "67c5b7e7003987ac953d",
            fileId = userId
        )
    } catch (e: Exception) {
        Log.w("ProfileUpload", "Failed to delete old avatar file", e)
    }

    return try {
        val result = AppwriteClient.storage.createFile(
            bucketId = "67c5b7e7003987ac953d",
            fileId = userId,
            file = InputFile.fromFile(file)
        )

        "https://cloud.appwrite.io/v1/storage/buckets/67c5b7e7003987ac953d/files/${result.id}/view?project=67c5b7900039311024c6&ts=${System.currentTimeMillis()}"
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        null
    }
}