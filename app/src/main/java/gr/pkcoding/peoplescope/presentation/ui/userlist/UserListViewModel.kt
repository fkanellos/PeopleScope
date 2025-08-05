package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.lifecycle.viewModelScope
import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.usecase.GetUsersUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.base.BaseViewModel
import gr.pkcoding.peoplescope.presentation.toUiText
import gr.pkcoding.peoplescope.utils.Constants
import kotlinx.coroutines.launch
import timber.log.Timber

class UserListViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase
) : BaseViewModel<UserListState, UserListIntent, UserListEffect>(
    UserListState()
) {

    init {
        processIntent(UserListIntent.LoadUsers)
    }

    override suspend fun handleIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.LoadUsers -> loadUsers()
            is UserListIntent.RefreshUsers -> refreshUsers()
            is UserListIntent.LoadMoreUsers -> loadMoreUsers()
            is UserListIntent.ToggleBookmark -> toggleBookmark(intent.user)
            is UserListIntent.NavigateToDetail -> navigateToDetail(intent.user)
            is UserListIntent.RetryLoadUsers -> loadUsers()
        }
    }

    private suspend fun loadUsers() {
        updateState { copy(isLoading = true, error = null) }

        getUsersUseCase(page = 1, pageSize = Constants.PAGE_SIZE).fold(
            onSuccess = { users ->
                updateState {
                    copy(
                        users = users,
                        isLoading = false,
                        currentPage = 1,
                        endReached = users.size < Constants.PAGE_SIZE
                    )
                }
            },
            onError = { error ->
                updateState {
                    copy(
                        isLoading = false,
                        error = error.toUiText()
                    )
                }
            }
        )
    }

    private suspend fun refreshUsers() {
        updateState { copy(isRefreshing = true, error = null) }

        getUsersUseCase(page = 1, pageSize = Constants.PAGE_SIZE).fold(
            onSuccess = { users ->
                updateState {
                    copy(
                        users = users,
                        isRefreshing = false,
                        currentPage = 1,
                        endReached = users.size < Constants.PAGE_SIZE
                    )
                }
            },
            onError = { error ->
                updateState {
                    copy(
                        isRefreshing = false,
                        error = error.toUiText()
                    )
                }
                sendEffect(UserListEffect.ShowError(error.toUiText()))
            }
        )
    }

    private suspend fun loadMoreUsers() {
        if (state.value.isLoadingMore || state.value.endReached) return

        updateState { copy(isLoadingMore = true) }

        val nextPage = state.value.currentPage + 1
        getUsersUseCase(page = nextPage, pageSize = Constants.PAGE_SIZE).fold(
            onSuccess = { newUsers ->
                updateState {
                    copy(
                        users = users + newUsers,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        endReached = newUsers.size < Constants.PAGE_SIZE
                    )
                }
            },
            onError = { error ->
                updateState { copy(isLoadingMore = false) }
                sendEffect(UserListEffect.ShowError(error.toUiText()))
            }
        )
    }

    private fun toggleBookmark(user: User) {
        viewModelScope.launch {
            toggleBookmarkUseCase(user).fold(
                onSuccess = {
                    // Update the user in the list
                    updateState {
                        copy(
                            users = users.map { u ->
                                if (u.id == user.id) {
                                    u.copy(isBookmarked = !u.isBookmarked)
                                } else {
                                    u
                                }
                            }
                        )
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
}

// Extension function for Result
inline fun <T, E : gr.pkcoding.peoplescope.domain.model.Error> gr.pkcoding.peoplescope.domain.model.Result<T, E>.fold(
    onSuccess: (T) -> Unit,
    onError: (E) -> Unit
) {
    when (this) {
        is gr.pkcoding.peoplescope.domain.model.Result.Success -> onSuccess(data)
        is gr.pkcoding.peoplescope.domain.model.Result.Error -> onError(error)
    }
}