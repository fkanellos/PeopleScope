package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.base.ViewState

data class UserListState(
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val cachedUsers: List<User> = emptyList(),
    val isOnline: Boolean = true,
    val isOfflineMode: Boolean = false,
    val showNetworkError: Boolean = false
) : ViewState
