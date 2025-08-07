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
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.BaseViewModel
import gr.pkcoding.peoplescope.presentation.toUiText
import gr.pkcoding.peoplescope.utils.Constants
import gr.pkcoding.peoplescope.utils.debounceSearch
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

    private val _searchQuery = MutableStateFlow("")
    private val _bookmarkedUserIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        Timber.d("🚀 UserListViewModel initialized")

        // Observe database changes
        viewModelScope.launch {
            bookmarkDao.getAllBookmarkedUsers().collect { bookmarkedUsers ->
                val bookmarkedIds = bookmarkedUsers.map { it.id }.toSet()
                Timber.d("📊 Database bookmark update: ${bookmarkedIds.size} bookmarks")
                _bookmarkedUserIds.value = bookmarkedIds
            }
        }

        // ✅ Debounced search query updates
        viewModelScope.launch {
            _searchQuery
                .debounceSearch(Constants.SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { debouncedQuery ->
                    Timber.d("🔍 Debounced search query: '$debouncedQuery'")
                    updateState { copy(searchQuery = debouncedQuery) }
                }
        }
    }

    // Base paging data flow
    private val pagedUsers: Flow<PagingData<User>> = getUsersPagedUseCase()
        .cachedIn(viewModelScope)

    val pagedUsersWithUpdates: Flow<PagingData<User>> = combine(
        pagedUsers,
        _bookmarkedUserIds,
        state.map { it.searchQuery }.distinctUntilChanged()
    ) { pagingData, bookmarkedIds, searchQuery ->

        Timber.d("🔄 Combining data: bookmarks=${bookmarkedIds.size}, search='$searchQuery'")

        pagingData
            .map { user ->
                // Update bookmark status
                val isBookmarked = user.id?.let { it in bookmarkedIds } ?: false
                user.copy(isBookmarked = isBookmarked)
            }
            .filter { user ->
                // ✅ Only show valid users
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

    override suspend fun handleIntent(intent: UserListIntent) {
        Timber.d("🎯 Handling intent: ${intent::class.simpleName}")

        when (intent) {
            is UserListIntent.ToggleBookmark -> toggleBookmark(intent.user)
            is UserListIntent.NavigateToDetail -> navigateToDetail(intent.user)
            is UserListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is UserListIntent.ClearSearch -> clearSearch()
        }
    }

    // ✅ Immediate UI update + debounced processing
    private fun updateSearchQuery(query: String) {
        Timber.d("🔍 Updating search query to: '$query'")
        updateState { copy(searchQuery = query) }
    }

    private fun clearSearch() {
        Timber.d("🧹 Clearing search query")
        _searchQuery.value = ""
    }

    private fun toggleBookmark(user: User) {
        if (!user.isValid() || user.id == null) {
            Timber.w("Cannot bookmark invalid user: ${user.getDisplayName()}")
            sendEffect(UserListEffect.ShowError(UiText.DynamicString("Cannot bookmark this user")))
            return
        }

        Timber.d("⭐ Toggling bookmark for user: ${user.getDisplayName()}")

        viewModelScope.launch {
            toggleBookmarkUseCase(user).fold(
                onSuccess = {
                    val newBookmarkState = !user.isBookmarked
                    Timber.d("✅ Successfully toggled bookmark for user: ${user.id}")

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
                    Timber.e("❌ Error toggling bookmark for user ${user.id}: $error")
                    sendEffect(UserListEffect.ShowError(error.toUiText()))
                }
            )
        }
    }

    private fun navigateToDetail(user: User) {
        // ✅ Double-check validation πριν το navigation
        if (!user.isValid() || user.id.isNullOrBlank()) {
            Timber.w("Cannot navigate to detail for invalid user: ${user.getDisplayName()}")
            sendEffect(UserListEffect.ShowError(UiText.DynamicString("Cannot view details for this user")))
            return
        }

        Timber.d("🚀 Navigating to detail for user: ${user.getDisplayName()} (ID: ${user.id})")
        sendEffect(UserListEffect.NavigateToUserDetail(user))
    }
}