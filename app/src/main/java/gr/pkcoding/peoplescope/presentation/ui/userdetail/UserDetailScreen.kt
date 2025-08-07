package gr.pkcoding.peoplescope.presentation.ui.userdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.presentation.ui.components.BookmarkButton
import gr.pkcoding.peoplescope.presentation.ui.components.ErrorView
import gr.pkcoding.peoplescope.presentation.ui.components.GradientBackground
import gr.pkcoding.peoplescope.presentation.ui.components.LoadingView
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import gr.pkcoding.peoplescope.utils.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    state: UserDetailState,
    onIntent: (UserDetailIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                state.isLoading -> {
                    LoadingView()
                }
                state.error != null -> {
                    ErrorView(
                        message = state.error.asString(),
                        onRetry = { /* Retry logic if needed */ }
                    )
                }
                state.user != null -> {
                    UserDetailContent(
                        user = state.user,
                        isBookmarked = state.isBookmarked,
                        onBookmarkClick = { onIntent(UserDetailIntent.ToggleBookmark) }
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
    onBookmarkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Section
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
                Box {
                    AsyncImage(
                        model = user.picture.large,
                        contentDescription = "${user.name.getFullName()} avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape),
                        color = if (user.gender == "male")
                            Color(0xFF2196F3) else Color(0xFFE91E63)
                    ) {
                        Text(
                            text = user.gender.take(1).uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.name.getFullName(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${user.dob.age} years old",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                BookmarkButton(
                    isBookmarked = isBookmarked,
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(48.dp),
                    tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                UserInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = user.email
                )

                UserInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = "${user.phone} / ${user.cell}"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                UserInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Address",
                    value = user.location.getFullAddress()
                )

                UserInfoRow(
                    icon = Icons.Default.Person,
                    label = "Nationality",
                    value = user.nationality.uppercase()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Additional Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SimpleInfoRow(
                    label = "Date of Birth",
                    value = user.dob.date.formatDate()
                )

                SimpleInfoRow(
                    label = "Timezone",
                    value = "${user.location.timezone.description} (${user.location.timezone.offset})"
                )
            }
        }
    }
}

@Composable
private fun UserInfoRow(
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