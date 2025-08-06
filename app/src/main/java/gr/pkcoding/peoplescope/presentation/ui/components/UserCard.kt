package gr.pkcoding.peoplescope.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme

@Stable
@Composable
fun UserCard(
    user: User,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = user.picture.thumbnail,
                contentDescription = "${user.name.getFullName()} avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = user.name.getFullName(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${user.location.city}, ${user.location.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bookmark Button
            BookmarkButton(
                isBookmarked = user.isBookmarked,
                onClick = onBookmarkClick,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserCardPreview() {
    PeopleScopeTheme {
        UserCard(
            user = User(
                id = "1",
                gender = "male",
                name = Name("Mr", "John", "Doe"),
                email = "john.doe@example.com",
                phone = "+1234567890",
                cell = "+0987654321",
                picture = Picture(
                    large = "https://randomuser.me/api/portraits/men/32.jpg",
                    medium = "https://randomuser.me/api/portraits/med/men/32.jpg",
                    thumbnail = "https://randomuser.me/api/portraits/thumb/men/32.jpg"
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
                dob = DateOfBirth("1990-01-01", 33),
                nationality = "US",
                isBookmarked = false
            ),
            onBookmarkClick = {},
            onClick = {}
        )
    }
}