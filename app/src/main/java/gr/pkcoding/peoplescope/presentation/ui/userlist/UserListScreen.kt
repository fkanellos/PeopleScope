package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.ui.components.ErrorView
import gr.pkcoding.peoplescope.presentation.ui.components.GradientBackground
import gr.pkcoding.peoplescope.presentation.ui.components.LoadingView
import gr.pkcoding.peoplescope.presentation.ui.components.SearchBar
import gr.pkcoding.peoplescope.presentation.ui.components.UserCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    state: UserListState,
    onIntent: (UserListIntent) -> Unit,
    viewModel: UserListViewModel
) {
    val lazyPagingItems = viewModel.getPagedUsersWithBookmarkUpdates().collectAsLazyPagingItems()
    val pullToRefreshState = rememberPullToRefreshState()

// 1. Trigger paging refresh if user pulls to refresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            lazyPagingItems.refresh()
        }
    }

// 2. Sync loading state with pullToRefresh UI
    LaunchedEffect(lazyPagingItems.loadState.refresh) {
        when (lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> pullToRefreshState.startRefresh()
            else -> pullToRefreshState.endRefresh()
        }
    }



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
            Box(
                modifier = Modifier
                    .fillMaxSize()
//                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                UserListContent(
                    lazyPagingItems = lazyPagingItems,
                    onIntent = onIntent
                )

//                PullToRefreshBox(
//                    state = pullToRefreshState,
//                    modifier = Modifier.align(Alignment.TopCenter)
//                )
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
            LoadingView(useShimmer = true)
        }
        lazyPagingItems.loadState.refresh is LoadState.Error && lazyPagingItems.itemCount == 0 -> {
            val error = (lazyPagingItems.loadState.refresh as LoadState.Error).error
            ErrorView(
                message = error.localizedMessage ?: "An error occurred",
                onRetry = { lazyPagingItems.retry() }
            )
        }
        else -> {
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