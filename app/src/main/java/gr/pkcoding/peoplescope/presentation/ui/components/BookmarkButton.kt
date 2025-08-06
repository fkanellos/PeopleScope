package gr.pkcoding.peoplescope.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import gr.pkcoding.peoplescope.R

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
) {
    AnimatedBookmarkButton(
        isBookmarked = isBookmarked,
        onClick = onClick,
        modifier = modifier,
        tint = tint
    )
}

@Preview
@Composable
private fun BookmarkButtonPreview() {
    BookmarkButton(
        isBookmarked = true,
        onClick = {}
    )
}