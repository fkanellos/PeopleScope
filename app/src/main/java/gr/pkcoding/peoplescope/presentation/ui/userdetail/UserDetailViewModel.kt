package gr.pkcoding.peoplescope.presentation.ui.userdetail

import androidx.lifecycle.viewModelScope
import gr.pkcoding.peoplescope.domain.usecase.GetUserDetailsUseCase
import gr.pkcoding.peoplescope.domain.usecase.IsUserBookmarkedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.base.BaseViewModel
import gr.pkcoding.peoplescope.presentation.toUiText
import gr.pkcoding.peoplescope.utils.fold
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class UserDetailViewModel(
    private val userId: String,
    private val getUserDetailsUseCase: GetUserDetailsUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val isUserBookmarkedUseCase: IsUserBookmarkedUseCase
) : BaseViewModel<UserDetailState, UserDetailIntent, UserDetailEffect>(
    UserDetailState()
) {

    init {
        processIntent(UserDetailIntent.LoadUser(userId))
        observeBookmarkStatus()
    }

    override suspend fun handleIntent(intent: UserDetailIntent) {
        when (intent) {
            is UserDetailIntent.LoadUser -> loadUser(intent.userId)
            is UserDetailIntent.ToggleBookmark -> toggleBookmark()
            is UserDetailIntent.NavigateBack -> navigateBack()
        }
    }

    private suspend fun loadUser(userId: String) {
        updateState { copy(isLoading = true, error = null) }

        getUserDetailsUseCase(userId).fold(
            onSuccess = { user ->
                updateState {
                    copy(
                        user = user,
                        isLoading = false,
                        isBookmarked = user.isBookmarked
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

    private fun toggleBookmark() {
        val currentUser = state.value.user ?: return

        viewModelScope.launch {
            Timber.d("Detail screen: Toggling bookmark for ${currentUser.name?.getFullName()}")

            toggleBookmarkUseCase(currentUser).fold(
                onSuccess = {
                    val newBookmarkStatus = !state.value.isBookmarked
                    Timber.d("Detail screen: Successfully toggled bookmark, new state: $newBookmarkStatus")

                    // Update local state immediately for UI responsiveness
                    updateState {
                        copy(
                            user = user?.copy(isBookmarked = newBookmarkStatus),
                            isBookmarked = newBookmarkStatus
                        )
                    }
                    sendEffect(UserDetailEffect.ShowBookmarkToggled(newBookmarkStatus))

                    // Database update will automatically sync with UserList via Flow observer
                },
                onError = { error ->
                    Timber.e("Detail screen: Error toggling bookmark: $error")
                    sendEffect(UserDetailEffect.ShowError(error.toUiText()))
                }
            )
        }
    }

    private fun observeBookmarkStatus() {
        viewModelScope.launch {
            isUserBookmarkedUseCase(userId).collectLatest { isBookmarked ->
                updateState { copy(isBookmarked = isBookmarked) }

                // Update user object if it exists
                state.value.user?.let { currentUser ->
                    if (currentUser.isBookmarked != isBookmarked) {
                        updateState {
                            copy(user = currentUser.copy(isBookmarked = isBookmarked))
                        }
                    }
                }
            }
        }
    }

    private fun navigateBack() {
        sendEffect(UserDetailEffect.NavigateBack)
    }
}