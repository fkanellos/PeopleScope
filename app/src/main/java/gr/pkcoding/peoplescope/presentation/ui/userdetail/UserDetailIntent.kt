package gr.pkcoding.peoplescope.presentation.ui.userdetail

import gr.pkcoding.peoplescope.presentation.base.ViewIntent

sealed class UserDetailIntent : ViewIntent {
    data class LoadUser(val userId: String) : UserDetailIntent()
    object ToggleBookmark : UserDetailIntent()
    object NavigateBack : UserDetailIntent()
}