package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.ViewEffect

sealed class UserListEffect : ViewEffect {
    data class NavigateToUserDetail(val user: User) : UserListEffect()
    data class ShowError(val message: UiText) : UserListEffect()
    data class ShowBookmarkToggled(val isBookmarked: Boolean) : UserListEffect()
}