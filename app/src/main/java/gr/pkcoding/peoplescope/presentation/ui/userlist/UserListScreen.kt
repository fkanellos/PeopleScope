package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.data.mapper.isNetworkRelatedError
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.ui.components.GradientBackground
import gr.pkcoding.peoplescope.presentation.ui.components.LoadingView
import gr.pkcoding.peoplescope.presentation.ui.components.SearchBar
import gr.pkcoding.peoplescope.presentation.ui.components.UserCard
import gr.pkcoding.peoplescope.presentation.ui.components.error_views.ErrorView
import gr.pkcoding.peoplescope.presentation.ui.components.error_views.NetworkStatusBar
import gr.pkcoding.peoplescope.presentation.ui.components.error_views.NoInternetErrorView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    state: UserListState,
    onIntent: (UserListIntent) -> Unit,
    viewModel: UserListViewModel
) {
    val listState = rememberLazyListState()

    val showFullTopAppBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 20
        }
    }

    val onSearchQueryChanged: (String) -> Unit = remember(onIntent) {
        { query -> onIntent(UserListIntent.UpdateSearchQuery(query)) }
    }

    val onClearSearch: () -> Unit = remember(onIntent) {
        { onIntent(UserListIntent.ClearSearch) }
    }

    val lazyPagingItems = remember(viewModel) {
        viewModel.pagedUsersWithUpdates
    }.collectAsLazyPagingItems()

    val topAppBarTitleResId by remember(state.searchQuery) {
        derivedStateOf {
            if (state.searchQuery.isBlank()) {
                R.string.users_title
            } else {
                R.string.searching_users_title
            }
        }
    }


    val appBarHeight by animateDpAsState(
        targetValue = if (showFullTopAppBar) 60.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "appBarHeight"
    )

    val appBarAlpha by animateFloatAsState(
        targetValue = if (showFullTopAppBar) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "appBarAlpha"
    )

    val searchBarOffset by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "searchBarOffset"
    )

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .wrapContentSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .zIndex(1f)
            ) {
                NetworkStatusBar(
                    isOnline = state.isOnline,
                    isOfflineMode = state.isOfflineMode
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(appBarHeight)
                        .alpha(appBarAlpha)
                ) {
                    if (appBarHeight > 0.dp) {
                        TopAppBar(
                            title = { Text(
                                text = stringResource(topAppBarTitleResId),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .wrapContentHeight(Alignment.CenterVertically)) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChanged,
                    onClearClick = onClearSearch,
                    modifier = Modifier
                        .offset(y = searchBarOffset)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(bottom = 4.dp)
                )
            }
        }
    ) { paddingValues ->
        GradientBackground(
            modifier = Modifier.padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading,
                onRefresh = { lazyPagingItems.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                UserListContent(
                    lazyPagingItems = lazyPagingItems,
                    onIntent = onIntent,
                    listState = listState
                )
            }
        }
    }
}

@Composable
private fun UserListContent(
    lazyPagingItems: LazyPagingItems<User>,
    onIntent: (UserListIntent) -> Unit,
    listState: LazyListState,
) {
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf {
            lazyPagingItems.itemCount > 10 && listState.firstVisibleItemIndex > 3
        }
    }

    val onToggleBookmark = remember(onIntent) {
        { user: User -> onIntent(UserListIntent.ToggleBookmark(user)) }
    }

    val onNavigateToDetail = remember(onIntent) {
        { user: User -> onIntent(UserListIntent.NavigateToDetail(user)) }
    }

    val onScrollToTop: () -> Unit = remember(listState, coroutineScope) {
        {
            coroutineScope.launch {
                if (listState.firstVisibleItemIndex > 50) {
                    listState.scrollToItem(50)
                }
                listState.animateScrollToItem(0)
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        when {
            lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                LoadingView(useShimmer = true)
            }

            lazyPagingItems.loadState.refresh is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                val error = (lazyPagingItems.loadState.refresh as LoadState.Error).error

                if (error.isNetworkRelatedError()) {
                    NoInternetErrorView(
                        onRetry = { lazyPagingItems.retry() }
                    )
                } else {
                    ErrorView(
                        message = error.localizedMessage ?: "An error occurred",
                        onRetry = { lazyPagingItems.retry() }
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = true
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { user ->
                            user.id ?: "invalid_${user.hashCode()}"
                        },
                        contentType = lazyPagingItems.itemContentType { "user" }
                    ) { index ->
                        val user = lazyPagingItems[index]

                        user?.takeIf { it.isValid() }?.let { validUser ->
                            UserCard(
                                user = validUser,
                                onBookmarkClick = { onToggleBookmark(validUser) },
                                onClick = { onNavigateToDetail(validUser) }
                            )
                        }
                    }

                    when (lazyPagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        is LoadState.Error -> {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Failed to load more users",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        TextButton(
                                            onClick = { lazyPagingItems.retry() },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("🔄 Retry")
                                        }
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = onScrollToTop,
                modifier = Modifier
                    .padding(16.dp)
                    .shadow(8.dp, CircleShape),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top"
                )
            }
        }
    }
}