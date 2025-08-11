package gr.pkcoding.peoplescope.presentation.ui.components.error_views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListState
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme

@Composable
fun EnhancedNetworkStatusBar(
    state: UserListState,
    onRetryClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = state.shouldShowOfflineContent() || state.shouldShowNetworkError(),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = when {
                state.shouldShowNetworkError() -> MaterialTheme.colorScheme.errorContainer
                state.isOfflineMode -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            animationSpec = tween(300),
            label = "background_color"
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor
        ) {
            when {
                state.shouldShowNetworkError() -> {
                    NetworkErrorBar(onRetryClick = onRetryClick)
                }
                state.isOfflineMode -> {
                    OfflineModeBar(
                        hasBookmarks = state.cachedUsers.isNotEmpty(),
                        onRefreshClick = onRefreshClick
                    )
                }
                state.isConnectionJustRestored() -> {
                    ConnectionRestoredBar(onRefreshClick = onRefreshClick)
                }
            }
        }
    }
}

@Composable
private fun NetworkErrorBar(
    onRetryClick: () -> Unit
) {
    StatusBarContent(
        icon = Icons.Default.WifiOff,
        iconColor = MaterialTheme.colorScheme.error,
        title = stringResource(R.string.no_internet_short),
        subtitle = stringResource(R.string.check_connection),
        actionText = stringResource(R.string.retry_with_icon),
        onActionClick = onRetryClick
    )
}

@Composable
private fun OfflineModeBar(
    hasBookmarks: Boolean,
    onRefreshClick: () -> Unit
) {
    val (title, subtitle) = if (hasBookmarks) {
        stringResource(R.string.offline_mode) to stringResource(R.string.showing_bookmarked_users)
    } else {
        stringResource(R.string.offline_mode) to stringResource(R.string.showing_cached_content)
    }

    StatusBarContent(
        icon = Icons.Default.CloudOff,
        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        title = title,
        subtitle = subtitle,
        actionText = stringResource(R.string.refresh),
        onActionClick = onRefreshClick
    )
}

@Composable
private fun ConnectionRestoredBar(
    onRefreshClick: () -> Unit
) {
    StatusBarContent(
        icon = Icons.Default.Wifi,
        iconColor = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.connected),
        subtitle = stringResource(R.string.pull_to_refresh_latest),
        actionText = stringResource(R.string.refresh),
        onActionClick = onRefreshClick
    )
}

@Composable
private fun StatusBarContent(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TextButton(
            onClick = onActionClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = iconColor
            )
        ) {
            Text(actionText)
        }
    }
}

@Composable
fun SmartNetworkIndicator(
    state: UserListState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !state.isOnline,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        state.shouldShowNetworkError() -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.shouldShowNetworkError()) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = if (state.shouldShowNetworkError()) {
                        stringResource(R.string.no_data)
                    } else {
                        stringResource(R.string.offline)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun NetworkStatusPreview() {
    PeopleScopeTheme {
        Column {
            EnhancedNetworkStatusBar(
                state = UserListState(
                    isOnline = false,
                    isOfflineMode = true,
                    showNetworkError = false
                )
            )

            SmartNetworkIndicator(
                state = UserListState(
                    isOnline = false,
                    showNetworkError = true
                )
            )
        }
    }
}