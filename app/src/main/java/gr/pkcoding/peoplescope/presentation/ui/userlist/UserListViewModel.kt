package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
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
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase
) : BaseViewModel<UserListState, UserListIntent, UserListEffect>(
    UserListState()
) {

    // Paging data flow
    val pagedUsers: Flow<PagingData<User>> = getUsersPagedUseCase()
        .cachedIn(viewModelScope)

    // Track bookmarked user IDs for real-time updates
    private val _bookmarkedUserIds = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun handleIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.ToggleBookmark -> toggleBookmark(intent.user)
            is UserListIntent.NavigateToDetail -> navigateToDetail(intent.user)
            is UserListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is UserListIntent.ClearSearch -> clearSearch()
            else -> { /* Handled by Paging 3 */ }
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
    }

    private fun clearSearch() {
        updateState { copy(searchQuery = "") }
    }

    private fun toggleBookmark(user: User) {
        viewModelScope.launch {
            toggleBookmarkUseCase(user).fold(
                onSuccess = {
                    // Update local bookmarked state
                    _bookmarkedUserIds.update { ids ->
                        if (user.isBookmarked) {
                            ids - user.id
                        } else {
                            ids + user.id
                        }
                    }
                    sendEffect(UserListEffect.ShowBookmarkToggled(!user.isBookmarked))
                },
                onError = { error ->
                    sendEffect(UserListEffect.ShowError(error.toUiText()))
                }
            )
        }
    }

    private fun navigateToDetail(user: User) {
        sendEffect(UserListEffect.NavigateToUserDetail(user))
    }

    // Combine paging data with bookmark updates and search
    fun getPagedUsersWithBookmarkUpdates(): Flow<PagingData<User>> {
        return combine(
            pagedUsers,
            _bookmarkedUserIds,
            state.map { it.searchQuery }.distinctUntilChanged()
        ) { pagingData, bookmarkedIds, searchQuery ->
            pagingData
                .map { user ->
                    // Update bookmark status based on local state
                    val isBookmarked = if (bookmarkedIds.contains(user.id)) {
                        true
                    } else if (_bookmarkedUserIds.value.isNotEmpty() && !bookmarkedIds.contains(user.id)) {
                        false
                    } else {
                        user.isBookmarked
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
        }
    }
}