package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.base.ViewIntent

sealed class UserListIntent : ViewIntent {
    data class ToggleBookmark(val user: User) : UserListIntent()
    data class NavigateToDetail(val user: User) : UserListIntent()
    data class UpdateSearchQuery(val query: String) : UserListIntent()
    object ClearSearch : UserListIntent()
    object RetryConnection : UserListIntent()
    object RefreshAfterReconnection : UserListIntent()
}