package com.example.classcrush.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.classcrush.data.model.Gender
import com.example.classcrush.presentation.viewmodel.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onNavigateToMain()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Progress Indicator
        LinearProgressIndicator(
            progress = (currentStep + 1) / 6f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        when (currentStep) {
            0 -> BasicInfoStep(
                name = uiState.name,
                age = uiState.age,
                onNameChange = viewModel::updateName,
                onAgeChange = viewModel::updateAge,
                onNext = { currentStep = 1 }
            )
            1 -> GenderStep(
                selectedGender = uiState.gender,
                onGenderSelected = viewModel::updateGender,
                onNext = { currentStep = 2 }
            )
            2 -> PreferencesStep(
                selectedPreference = uiState.interestedIn,
                onPreferenceSelected = viewModel::updateInterestedIn,
                onNext = { currentStep = 3 }
            )
            3 -> CollegeInfoStep(
                department = uiState.department,
                year = uiState.year,
                college = uiState.college,
                onDepartmentChange = viewModel::updateDepartment,
                onYearChange = viewModel::updateYear,
                onCollegeChange = viewModel::updateCollege,
                onNext = { currentStep = 4 }
            )
            4 -> PhotoStep(
                profileImageUri = uiState.profileImageUri,
                onImageSelected = viewModel::updateProfileImage,
                onNext = { currentStep = 5 }
            )
            5 -> BioStep(
                bio = uiState.bio,
                onBioChange = viewModel::updateBio,
                onComplete = viewModel::completeOnboarding,
                isLoading = uiState.isLoading
            )
        }

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            if (currentStep < 5) {
                Button(
                    onClick = { currentStep++ },
                    enabled = when (currentStep) {
                        0 -> uiState.name.isNotBlank() && uiState.age >= 18
                        1 -> true
                        2 -> true
                        3 -> uiState.department.isNotBlank() && uiState.year.isNotBlank() && uiState.college.isNotBlank()
                        4 -> uiState.profileImageUri != null
                        else -> true
                    }
                ) {
                    Text("Next")
                }
            }
        }

        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoStep(
    name: String,
    age: Int,
    onNameChange: (String) -> Unit,
    onAgeChange: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "What's your name?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("First Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = if (age == 0) "" else age.toString(),
            onValueChange = {
                val newAge = it.toIntOrNull() ?: 0
                if (newAge in 0..100) onAgeChange(newAge)
            },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun GenderStep(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "I am a",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val genders = listOf(
            Gender.MALE to "Man",
            Gender.FEMALE to "Woman",
            Gender.OTHER to "Non-binary"
        )

        genders.forEach { (gender, label) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onGenderSelected(gender) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedGender == gender)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (selectedGender == gender)
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PreferencesStep(
    selectedPreference: Gender,
    onPreferenceSelected: (Gender) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Show me",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val preferences = listOf(
            Gender.MALE to "Men",
            Gender.FEMALE to "Women",
            Gender.ALL to "Everyone"
        )

        preferences.forEach { (preference, label) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onPreferenceSelected(preference) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedPreference == preference)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (selectedPreference == preference)
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeInfoStep(
    department: String,
    year: String,
    college: String,
    onDepartmentChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onCollegeChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Tell us about your studies",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = college,
            onValueChange = onCollegeChange,
            label = { Text("College/University") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = department,
            onValueChange = onDepartmentChange,
            label = { Text("Department/Major") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = year,
            onValueChange = onYearChange,
            label = { Text("Year (e.g., Freshman, Sophomore)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun PhotoStep(
    profileImageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onNext: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add your best photo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(profileImageUri),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add photo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add Photo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Text(
            text = "Choose a photo that shows your face clearly",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioStep(
    bio: String,
    onBioChange: (String) -> Unit,
    onComplete: () -> Unit,
    isLoading: Boolean
) {
    Column {
        Text(
            text = "Tell us about yourself",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            placeholder = { Text("Write something interesting about yourself...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Text(
            text = "${bio.length}/500",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Complete Profile", fontSize = 16.sp)
            }
        }
    }
}
