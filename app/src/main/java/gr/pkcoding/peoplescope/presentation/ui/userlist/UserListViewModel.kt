package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.BaseViewModel
import gr.pkcoding.peoplescope.presentation.toUiText
import gr.pkcoding.peoplescope.utils.Constants
import gr.pkcoding.peoplescope.utils.debounceSearch
import gr.pkcoding.peoplescope.utils.fold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class UserListViewModel(
    private val getUsersPagedUseCase: GetUsersPagedUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val bookmarkDao: BookmarkDao,
    private val networkProvider: NetworkConnectivityProvider
) : BaseViewModel<UserListState, UserListIntent, UserListEffect>(
    UserListState()
) {

    private val _searchQuery = MutableStateFlow("")
    private val _bookmarkedUserIds = MutableStateFlow<Set<String>>(emptySet())
    private val _currentUserCache = MutableStateFlow<List<User>>(emptyList())

    init {
        Timber.d("UserListViewModel initialized")

        observeBookmarkChanges()
        observeNetworkChanges()
        observeSearchChanges()
    }

    private fun observeBookmarkChanges() {
        viewModelScope.launch {
            bookmarkDao.getAllBookmarkedUsers().collect { bookmarkedUsers ->
                val bookmarkedIds = bookmarkedUsers.map { it.id }.toSet()
                Timber.d("Database bookmark update: ${bookmarkedIds.size} bookmarks")
                _bookmarkedUserIds.value = bookmarkedIds
            }
        }
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkProvider.networkConnectivityFlow()
                .distinctUntilChanged() // Prevent rapid toggles
                .collect { isOnline ->
                    Timber.d("Network state changed: $isOnline")
                    handleNetworkStateChange(isOnline)
                }
        }
    }

    private fun observeSearchChanges() {
        viewModelScope.launch {
            _searchQuery
                .debounceSearch(Constants.SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { debouncedQuery ->
                    Timber.d("Debounced search query: '$debouncedQuery'")
                    updateState { copy(searchQuery = debouncedQuery) }
                }
        }
    }

    private fun handleNetworkStateChange(isOnline: Boolean) {
        val currentState = state.value
        val hasBookmarkedUsers = _bookmarkedUserIds.value.isNotEmpty()
        val hasContent = currentState.cachedUsers.isNotEmpty()

        updateState {
            copy(
                isOnline = isOnline,
                lastOnlineState = this.isOnline, // Track previous state
                connectionChangeTimestamp = System.currentTimeMillis(),

                // Smart offline mode detection
                isOfflineMode = !isOnline && (hasBookmarkedUsers || hasContent),
                showNetworkError = !isOnline && !hasBookmarkedUsers && !hasContent
            )
        }

        val newState = state.value
        when {
            // Connection was just restored
            newState.isConnectionJustRestored() -> {
                Timber.d("Connection restored")
                sendEffect(UserListEffect.ConnectionRestored)

                // If we were showing error, auto-clear it
                if (currentState.showNetworkError) {
                    sendEffect(UserListEffect.ShowRefreshOption(
                        UiText.StringResource(R.string.connection_restored_pull_refresh)
                    ))
                }
            }

            // Connection was just lost - USE HELPER METHOD
            newState.isConnectionJustLost() -> {
                Timber.d("📵 Connection lost")
                sendEffect(UserListEffect.ConnectionLost)

                // Show appropriate message based on available content
                when {
                    hasBookmarkedUsers -> {
                        sendEffect(UserListEffect.ShowRefreshOption(
                            UiText.StringResource(R.string.no_internet_showing_bookmarks)
                        ))
                    }
                    hasContent -> {
                        sendEffect(UserListEffect.ShowRefreshOption(
                            UiText.StringResource(R.string.no_internet_showing_cached)
                        ))
                    }
                    else -> {
                        sendEffect(UserListEffect.ShowError(
                            UiText.StringResource(R.string.no_internet_no_cached_content)
                        ))
                    }
                }
            }
        }
    }

    private val pagedUsers: Flow<PagingData<User>> = getUsersPagedUseCase()
        .cachedIn(viewModelScope)

    val pagedUsersWithUpdates: Flow<PagingData<User>> = combine(
        pagedUsers,
        _bookmarkedUserIds,
        state.map { it.searchQuery }.distinctUntilChanged()
    ) { pagingData, bookmarkedIds, searchQuery ->

        Timber.d("Combining data: bookmarks=${bookmarkedIds.size}, search='$searchQuery'")

        pagingData
            .map { user ->
                // Update bookmark status
                val isBookmarked = user.id?.let { it in bookmarkedIds } ?: false
                val updatedUser = user.copy(isBookmarked = isBookmarked)

                updateCachedUsers(updatedUser)

                updatedUser
            }
            .filter { user ->
                // Only show valid users
                if (!user.isValid()) {
                    Timber.w("Filtering out invalid user: ${user.getDisplayName()}")
                    return@filter false
                }

                // Filter by search query
                if (searchQuery.isEmpty()) {
                    true
                } else {
                    val queryLower = searchQuery.lowercase()
                    user.name?.let { name ->
                        name.getFullName().lowercase().contains(queryLower)
                    } == true ||
                            user.email?.lowercase()?.contains(queryLower) == true ||
                            user.location?.city?.lowercase()?.contains(queryLower) == true ||
                            user.location?.country?.lowercase()?.contains(queryLower) == true
                }
            }
    }.cachedIn(viewModelScope)

    private fun updateCachedUsers(user: User) {
        _currentUserCache.update { currentCache ->
            val updatedCache = currentCache.toMutableList()
            val existingIndex = updatedCache.indexOfFirst { it.id == user.id }

            if (existingIndex >= 0) {
                updatedCache[existingIndex] = user
            } else {
                updatedCache.add(user)
            }

            // Keep cache size reasonable
            if (updatedCache.size > 200) {
                updatedCache.take(200)
            } else {
                updatedCache
            }
        }

        // Update state with cached users
        updateState {
            copy(cachedUsers = _currentUserCache.value)
        }
    }

    override suspend fun handleIntent(intent: UserListIntent) {
        Timber.d("Handling intent: ${intent::class.simpleName}")

        when (intent) {
            is UserListIntent.ToggleBookmark -> toggleBookmark(intent.user)
            is UserListIntent.NavigateToDetail -> navigateToDetail(intent.user)
            is UserListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is UserListIntent.ClearSearch -> clearSearch()
            is UserListIntent.RetryConnection -> handleRetryConnection()
            is UserListIntent.RefreshAfterReconnection -> handleRefreshAfterReconnection()
        }
    }

    private fun updateSearchQuery(query: String) {
        Timber.d("Updating search query to: '$query'")
        _searchQuery.value = query
        updateState { copy(searchQuery = query) }
    }

    private fun clearSearch() {
        Timber.d("Clearing search query")
        _searchQuery.value = ""
        updateState { copy(searchQuery = "") }
    }

    private fun handleRetryConnection() {
        if (networkProvider.isNetworkAvailable()) {
            sendEffect(UserListEffect.ShowRefreshOption(
                UiText.StringResource(R.string.connection_restored_pull_refresh)
            ))
        } else {
            sendEffect(UserListEffect.ShowError(
                UiText.StringResource(R.string.still_no_internet_connection)
            ))
        }
    }

    private fun handleRefreshAfterReconnection() {
        // This could trigger a refresh of the paging source
        // For now, we'll just show a confirmation
        sendEffect(UserListEffect.ShowRefreshOption(
            UiText.StringResource(R.string.refreshing_latest_data)
        ))
    }

    private fun toggleBookmark(user: User) {
        if (!user.isValid() || user.id == null) {
            Timber.w("Cannot bookmark invalid user: ${user.getDisplayName()}")
            sendEffect(UserListEffect.ShowError(UiText.StringResource(R.string.cannot_bookmark_user)))
            return
        }

        Timber.d("Toggling bookmark for user: ${user.getDisplayName()}")

        viewModelScope.launch {
            toggleBookmarkUseCase(user).fold(
                onSuccess = {
                    val newBookmarkState = !user.isBookmarked
                    Timber.d("Successfully toggled bookmark for user: ${user.id}")

                    _bookmarkedUserIds.update { ids ->
                        if (user.isBookmarked) {
                            ids - user.id
                        } else {
                            ids + user.id
                        }
                    }

                    sendEffect(UserListEffect.ShowBookmarkToggled(newBookmarkState))
                },
                onError = { error ->
                    Timber.e("Error toggling bookmark for user ${user.id}: $error")
                    sendEffect(UserListEffect.ShowError(error.toUiText()))
                }
            )
        }
    }

    private fun navigateToDetail(user: User) {
        if (!user.isValid() || user.id.isNullOrBlank()) {
            Timber.w("Cannot navigate to detail for invalid user: ${user.getDisplayName()}")
            sendEffect(UserListEffect.ShowError(UiText.StringResource(R.string.cannot_view_user_details)))
            return
        }

        Timber.d("Navigating to detail for user: ${user.getDisplayName()} (ID: ${user.id})")
        sendEffect(UserListEffect.NavigateToUserDetail(user))
    }
}