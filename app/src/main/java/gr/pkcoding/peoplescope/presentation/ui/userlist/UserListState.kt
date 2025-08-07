package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.Error
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.ViewIntent
import gr.pkcoding.peoplescope.presentation.base.ViewState

data class UserListState(
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val cachedUsers: List<User> = emptyList()
) : ViewState

// Extension function for Result
inline fun <T, E : Error> Result<T, E>.fold(
    onSuccess: (T) -> Unit,
    onError: (E) -> Unit
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(error)
    }
}