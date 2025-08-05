package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.base.ViewIntent

sealed class UserListIntent : ViewIntent {
    object LoadUsers : UserListIntent()
    object RefreshUsers : UserListIntent()
    object LoadMoreUsers : UserListIntent()
    data class ToggleBookmark(val user: User) : UserListIntent()
    data class NavigateToDetail(val user: User) : UserListIntent()
    object RetryLoadUsers : UserListIntent()
}