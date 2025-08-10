package gr.pkcoding.peoplescope.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gr.pkcoding.peoplescope.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedBookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
) {
    var animationPlayed by remember { mutableStateOf(false) }

    // Trigger animation when bookmark state changes to true
    LaunchedEffect(isBookmarked) {
        if (isBookmarked && !animationPlayed) {
            animationPlayed = true
        }
    }

    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isBookmarked && animationPlayed) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bookmark_scale"
    )

    // Rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (isBookmarked && animationPlayed) 360f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "bookmark_rotation"
    )

    IconButton(
        onClick = {
            onClick()
            if (!isBookmarked) {
                animationPlayed = false
            }
        },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = isBookmarked,
            transitionSpec = {
                if (targetState) {
                    // Entering animation for filled heart
                    scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn() togetherWith  scaleOut() + fadeOut()
                } else {
                    // Exiting animation
                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                }
            },
            contentAlignment = Alignment.Center,
            label = "bookmark_icon"
        ) { bookmarked ->
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(
                    id = if (bookmarked) R.string.unbookmark else R.string.bookmark
                ),
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = if (bookmarked) scale else 1f
                        scaleY = if (bookmarked) scale else 1f
                        rotationY = if (bookmarked) rotation else 0f
                    }
            )
        }
    }
}
