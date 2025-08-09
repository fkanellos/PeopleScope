package gr.pkcoding.peoplescope.presentation.ui.userdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.presentation.ui.components.BookmarkButton
import gr.pkcoding.peoplescope.presentation.ui.components.error_views.ErrorView
import gr.pkcoding.peoplescope.presentation.ui.components.GradientBackground
import gr.pkcoding.peoplescope.presentation.ui.components.LoadingView
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import gr.pkcoding.peoplescope.utils.formatDate
import gr.pkcoding.peoplescope.utils.showToast
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    state: UserDetailState,
    onIntent: (UserDetailIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentUser = state.user
    val currentError = state.error

    // ✅ Memoized copyEmail
    val onCopyEmail = remember(currentUser?.email) {
        {
            currentUser?.email?.takeIf { it.isNotBlank() }?.let { email ->
                clipboardManager.setText(AnnotatedString(email))
                context.showToast("📧 Email copied to clipboard")
                Timber.d("📋 Email copied: $email")
            } ?: context.showToast("No email available")
        }
    }

    // ✅ Memoized copyPhone
    val onCopyPhone = remember(currentUser?.phone, currentUser?.cell) {
        {
            val phoneNumbers = listOfNotNull(
                currentUser?.phone?.takeIf { it.isNotBlank() },
                currentUser?.cell?.takeIf { it.isNotBlank() }
            )
            if (phoneNumbers.isNotEmpty()) {
                val phoneText = phoneNumbers.joinToString(" • ")
                clipboardManager.setText(AnnotatedString(phoneText))
                context.showToast("📞 Phone numbers copied to clipboard")
                Timber.d("📋 Phone copied: $phoneText")
            } else {
                context.showToast("No phone numbers available")
            }
        }
    }

    val onToggleBookmark = remember(onIntent) {
        { onIntent(UserDetailIntent.ToggleBookmark) }
    }

    val onRetryLoadUser = remember(onIntent, currentUser?.id) {
        {
            currentUser?.id?.let { userId ->
                onIntent(UserDetailIntent.LoadUser(userId))
            } ?: Unit
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        GradientBackground(
            modifier = Modifier.padding(paddingValues)
        ) {
            when {
                state.isLoading -> LoadingView()

                currentError != null -> {
                    ErrorView(
                        message = currentError.asString(),
                        onRetry = onRetryLoadUser
                    )
                }

                currentUser != null -> {
                    UserDetailContent(
                        user = currentUser,
                        isBookmarked = state.isBookmarked,
                        onBookmarkClick = onToggleBookmark,
                        onCopyEmail = onCopyEmail,
                        onCopyPhone = onCopyPhone
                    )
                }
            }
        }
    }
}

@Composable
private fun UserDetailContent(
    user: User,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCopyEmail: () -> Unit,
    onCopyPhone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Section
        ProfileCard(
            user = user,
            isBookmarked = isBookmarked,
            onBookmarkClick = onBookmarkClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ Contact Information με copy buttons
        ContactInfoCard(
            user = user,
            onCopyEmail = onCopyEmail,
            onCopyPhone = onCopyPhone
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Location Information
        LocationInfoCard(user = user)

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Information
        AdditionalInfoCard(user = user)
    }
}

@Composable
private fun ContactInfoCard(
    user: User,
    onCopyEmail: () -> Unit,
    onCopyPhone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ✅ Email με copy button
            user.email?.takeIf { it.isNotBlank() }?.let { email ->
                ContactInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email,
                    onCopy = onCopyEmail
                )
            }

            // ✅ Phone numbers με copy button
            val phoneNumbers = listOfNotNull(
                user.phone?.takeIf { it.isNotBlank() },
                user.cell?.takeIf { it.isNotBlank() }
            )

            if (phoneNumbers.isNotEmpty()) {
                ContactInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone${if (phoneNumbers.size > 1) "s" else ""}",
                    value = phoneNumbers.joinToString(" • "),
                    onCopy = onCopyPhone
                )
            }
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ✅ Copy button με available Material Icon
        IconButton(
            onClick = onCopy,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.content_copy_24px),
                contentDescription = "Copy $label",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileCard(
    user: User,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile picture with gender indicator
            Box {
                AsyncImage(
                    model = user.picture?.large ?: user.picture?.medium,
                    contentDescription = "${user.getDisplayName()} avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )

                // Gender indicator
                user.gender?.let { gender ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape),
                        color = if (gender.lowercase() == "male")
                            Color(0xFF2196F3) else Color(0xFFE91E63)
                    ) {
                        Text(
                            text = gender.take(1).uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user.getDisplayName(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            user.dob?.age?.let { age ->
                Text(
                    text = "$age years old",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BookmarkButton(
                isBookmarked = isBookmarked,
                onClick = onBookmarkClick,
                modifier = Modifier.size(48.dp),
                tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LocationInfoCard(user: User) {
    if (user.location == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = user.location.getDisplayLocation().ifBlank { "Not available" }
            )

            user.nationality?.takeIf { it.isNotBlank() }?.let { nationality ->
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Nationality",
                    value = nationality.uppercase()
                )
            }
        }
    }
}

@Composable
private fun AdditionalInfoCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            user.dob?.date?.let { dobDate ->
                SimpleInfoRow(
                    label = "Date of Birth",
                    value = dobDate.formatDate()
                )
            }

            user.location?.timezone?.let { timezone ->
                val timezoneText = listOfNotNull(
                    timezone.description?.takeIf { it.isNotBlank() },
                    timezone.offset?.takeIf { it.isNotBlank() }?.let { "($it)" }
                ).joinToString(" ")

                if (timezoneText.isNotBlank()) {
                    SimpleInfoRow(
                        label = "Timezone",
                        value = timezoneText
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SimpleInfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailScreenPreview() {
    PeopleScopeTheme {
        UserDetailScreen(
            state = UserDetailState(
                user = User(
                    id = "1",
                    gender = "female",
                    name = Name("Ms", "Jane", "Doe"),
                    email = "jane.doe@example.com",
                    phone = "+1234567890",
                    cell = "+0987654321",
                    picture = Picture(
                        large = "https://randomuser.me/api/portraits/women/32.jpg",
                        medium = "https://randomuser.me/api/portraits/med/women/32.jpg",
                        thumbnail = "https://randomuser.me/api/portraits/thumb/women/32.jpg"
                    ),
                    location = Location(
                        street = Street(123, "Main St"),
                        city = "New York",
                        state = "NY",
                        country = "USA",
                        postcode = "10001",
                        coordinates = Coordinates("40.7128", "-74.0060"),
                        timezone = Timezone("-5:00", "Eastern Time")
                    ),
                    dob = DateOfBirth("1990-01-01T00:00:00.000Z", 33),
                    nationality = "US",
                    isBookmarked = true
                ),
                isBookmarked = true,
                isLoading = false,
                error = null
            ),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}