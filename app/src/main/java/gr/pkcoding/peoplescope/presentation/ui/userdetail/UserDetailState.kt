package gr.pkcoding.peoplescope.presentation.ui.userdetail

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.presentation.base.ViewState

data class UserDetailState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isBookmarked: Boolean = false
) : ViewState