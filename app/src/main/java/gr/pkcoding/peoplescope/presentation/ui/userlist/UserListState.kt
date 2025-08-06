package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.ViewIntent
import gr.pkcoding.peoplescope.presentation.base.ViewState

data class UserListState(
    val isRefreshing: Boolean = false,
    val searchQuery: String = ""
) : ViewState

data class UserListStateWithSearch(
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val cachedUsers: List<User> = emptyList() // For search filtering
) : ViewState

// Add search intent
sealed class UserListSearchIntent : ViewIntent {
    data class UpdateSearchQuery(val query: String) : UserListSearchIntent()
    object ClearSearch : UserListSearchIntent()
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