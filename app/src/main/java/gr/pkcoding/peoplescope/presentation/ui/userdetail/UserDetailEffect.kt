package gr.pkcoding.peoplescope.presentation.ui.userdetail

import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.ViewEffect

sealed class UserDetailEffect : ViewEffect {
    object NavigateBack : UserDetailEffect()
    data class ShowError(val message: UiText) : UserDetailEffect()
    data class ShowBookmarkToggled(val isBookmarked: Boolean) : UserDetailEffect()
}