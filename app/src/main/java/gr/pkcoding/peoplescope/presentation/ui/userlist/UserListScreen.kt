package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    state: UserListState,
    onIntent: (UserListIntent) -> Unit,
    viewModel: UserListViewModel
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // 🔥 USE THE PROPERTY DIRECTLY - NOT A FUNCTION!
    val lazyPagingItems = remember(viewModel) {
        viewModel.pagedUsersWithUpdates
    }.collectAsLazyPagingItems()

    // Add lifecycle awareness to pause data loading when app goes to background
    LaunchedEffect(lifecycle) {
        Timber.d("🔄 UserListScreen lifecycle changed")
    }

    // Determine if we should show refresh indicator
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.users_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { query ->
                        onIntent(UserListIntent.UpdateSearchQuery(query))
                    },
                    onClearClick = {
                        onIntent(UserListIntent.ClearSearch)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        GradientBackground(
            modifier = Modifier.padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    Timber.d("🔄 Pull to refresh triggered")
                    lazyPagingItems.refresh()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                UserListContent(
                    lazyPagingItems = lazyPagingItems,
                    onIntent = onIntent
                )
            }
        }
    }
}

@Composable
private fun UserListContent(
    lazyPagingItems: LazyPagingItems<User>,
    onIntent: (UserListIntent) -> Unit
) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id },
                    contentType = lazyPagingItems.itemContentType { "user" }
                ) { index ->
                    val user = lazyPagingItems[index]
                    user?.let {
                        UserCard(
                            user = it,
                            onBookmarkClick = {
                                onIntent(UserListIntent.ToggleBookmark(it))
                            },
                            onClick = {
                                onIntent(UserListIntent.NavigateToDetail(it))
                            }
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
}