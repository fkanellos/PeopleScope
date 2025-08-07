package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.base.BaseViewModel
import gr.pkcoding.peoplescope.presentation.toUiText
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class UserListViewModel(
    private val getUsersPagedUseCase: GetUsersPagedUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val bookmarkDao: BookmarkDao
) : BaseViewModel<UserListState, UserListIntent, UserListEffect>(
    UserListState()
) {

    // Track bookmarked user IDs for real-time updates - observe from database
    private val _bookmarkedUserIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        Timber.d("🚀 UserListViewModel initialized")

        // Observe database changes from ALL sources (list + detail screens)
        viewModelScope.launch {
            // Get all bookmarked users from database and map to IDs
            // This will update whenever ANY screen changes bookmarks
            bookmarkDao.getAllBookmarkedUsers().collect { bookmarkedUsers ->
                val bookmarkedIds = bookmarkedUsers.map { it.id }.toSet()
                Timber.d("📊 Database bookmark update: ${bookmarkedIds.size} bookmarks")
                _bookmarkedUserIds.value = bookmarkedIds
            }
        }
    }

    // Base paging data flow
    private val pagedUsers: Flow<PagingData<User>> = getUsersPagedUseCase()
        .cachedIn(viewModelScope)
        .onEach {
            Timber.d("📦 Base paging data received")
        }

    // 🔥 CREATE THE COMBINED FLOW ONCE - NOT IN A FUNCTION!
    val pagedUsersWithUpdates: Flow<PagingData<User>> = combine(
        pagedUsers,
        _bookmarkedUserIds,
        state.map { it.searchQuery }.distinctUntilChanged()
    ) { pagingData, bookmarkedIds, searchQuery ->

        Timber.d("🔄 Combining data: bookmarks=${bookmarkedIds.size}, search='$searchQuery'")

        pagingData
            .map { user ->
                // Update bookmark status based on local state
                val isBookmarked = when {
                    bookmarkedIds.contains(user.id) -> true
                    _bookmarkedUserIds.value.isNotEmpty() && !bookmarkedIds.contains(user.id) -> false
                    else -> user.isBookmarked
                }
                user.copy(isBookmarked = isBookmarked)
            }
            .filter { user ->
                // Filter by search query
                if (searchQuery.isEmpty()) {
                    true
                } else {
                    user.name.getFullName().contains(searchQuery, ignoreCase = true) ||
                            user.email.contains(searchQuery, ignoreCase = true) ||
                            user.location.city.contains(searchQuery, ignoreCase = true) ||
                            user.location.country.contains(searchQuery, ignoreCase = true)
                }
            }
    }.onEach {
        Timber.d("✅ Final paging data emitted")
    }.cachedIn(viewModelScope) // Cache the combined flow too

    init {
        Timber.d("🚀 UserListViewModel initialized")
    }

    override suspend fun handleIntent(intent: UserListIntent) {
        Timber.d("🎯 Handling intent: ${intent::class.simpleName}")

        when (intent) {
            is UserListIntent.ToggleBookmark -> toggleBookmark(intent.user)
            is UserListIntent.NavigateToDetail -> navigateToDetail(intent.user)
            is UserListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is UserListIntent.ClearSearch -> clearSearch()
        }
    }

    private fun updateSearchQuery(query: String) {
        Timber.d("🔍 Updating search query to: '$query'")
        updateState { copy(searchQuery = query) }
    }

    private fun clearSearch() {
        Timber.d("🧹 Clearing search query")
        updateState { copy(searchQuery = "") }
    }

    private fun toggleBookmark(user: User) {
        Timber.d("⭐ Toggling bookmark for user: ${user.name.getFullName()}, current state: ${user.isBookmarked}")

        viewModelScope.launch {
            toggleBookmarkUseCase(user).fold(
                onSuccess = {
                    val newBookmarkState = !user.isBookmarked
                    Timber.d("✅ Successfully toggled bookmark for user: ${user.id}, new state: $newBookmarkState")

                    // Remove manual state update - database observer will handle this
                    sendEffect(UserListEffect.ShowBookmarkToggled(newBookmarkState))
                },
                onError = { error ->
                    Timber.e("❌ Error toggling bookmark for user ${user.id}: $error")
                    sendEffect(UserListEffect.ShowError(error.toUiText()))
                }
            )
        }
    }

    private fun navigateToDetail(user: User) {
        Timber.d("🚀 Navigating to detail for user: ${user.name.getFullName()}")
        sendEffect(UserListEffect.NavigateToUserDetail(user))
    }
}