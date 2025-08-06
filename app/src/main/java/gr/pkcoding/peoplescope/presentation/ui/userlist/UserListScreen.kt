package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.ui.components.ErrorView
import gr.pkcoding.peoplescope.presentation.ui.components.GradientBackground
import gr.pkcoding.peoplescope.presentation.ui.components.LoadingView
import gr.pkcoding.peoplescope.presentation.ui.components.SearchBar
import gr.pkcoding.peoplescope.presentation.ui.components.UserCard
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    state: UserListState,
    onIntent: (UserListIntent) -> Unit,
    viewModel: UserListViewModel
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val lazyPagingItems = remember(viewModel) {
        viewModel.pagedUsersWithUpdates
    }.collectAsLazyPagingItems()

    LaunchedEffect(lifecycle) {
        Timber.d("🔄 UserListScreen lifecycle changed")
    }
    val onSearchQueryChanged: (String) -> Unit = remember(onIntent) {
        { query -> onIntent(UserListIntent.UpdateSearchQuery(query)) }
    }

    val onClearSearch: () -> Unit = remember(onIntent) {
        { onIntent(UserListIntent.ClearSearch) }
    }

    val onRefresh: () -> Unit = remember(lazyPagingItems) {
        {
            Timber.d("🔄 Pull to refresh triggered")
            lazyPagingItems.refresh()
        }
    }

    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .wrapContentSize()
                    .background(Color.Transparent)
                    .zIndex(1f)
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.users_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChanged,
                    onClearClick = onClearSearch,
                    modifier = Modifier
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
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    UserListContent(
                        lazyPagingItems = lazyPagingItems,
                        onIntent = onIntent
                    )
                }
            }
        }
    }
}

@Composable
private fun UserListContent(
    lazyPagingItems: LazyPagingItems<User>,
    onIntent: (UserListIntent) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Show scroll-to-top button when: itemCount > 10 AND scrolled past item 3
    val showScrollToTop by remember {
        derivedStateOf {
            lazyPagingItems.itemCount > 10 && listState.firstVisibleItemIndex > 3
        }
    }

    val onToggleBookmark: (User) -> Unit = remember(onIntent) {
        { user -> onIntent(UserListIntent.ToggleBookmark(user)) }
    }

    val onNavigateToDetail: (User) -> Unit = remember(onIntent) {
        { user -> onIntent(UserListIntent.NavigateToDetail(user)) }
    }

    val onScrollToTop: () -> Unit = remember(listState, coroutineScope) {
        {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                Timber.d("📱 Showing loading state")
                LoadingView(useShimmer = true)
            }

            lazyPagingItems.loadState.refresh is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
                val error = (lazyPagingItems.loadState.refresh as LoadState.Error).error
                Timber.e("❌ Showing error state: ${error.message}")
                ErrorView(
                    message = error.localizedMessage ?: "An error occurred",
                    onRetry = { lazyPagingItems.retry() }
                )
            }

            else -> {
                Timber.d("📋 Showing user list: ${lazyPagingItems.itemCount} items")
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = true
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { user -> user.id },
                        contentType = lazyPagingItems.itemContentType { "user" }
                    ) { index ->
                        val user = lazyPagingItems[index]
                        user?.let {
                            UserCard(
                                user = it,
                                onBookmarkClick = { onToggleBookmark(it) },
                                onClick = { onNavigateToDetail(it) }
                            )
                        }
                    }

                    // Loading more indicator
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(
                                        onClick = { lazyPagingItems.retry() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Retry")
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