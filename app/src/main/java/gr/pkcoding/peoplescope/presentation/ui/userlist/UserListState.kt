// File: app/src/main/java/gr/pkcoding/peoplescope/presentation/ui/userlist/UserListState.kt

package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.presentation.base.ViewState

data class UserListState(
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val cachedUsers: List<User> = emptyList(), // ✅ FIXED - Will be populated
    val isOnline: Boolean = true,
    val isOfflineMode: Boolean = false,
    val showNetworkError: Boolean = false,
    val lastOnlineState: Boolean = true, // Track previous state for smooth transitions
    val connectionChangeTimestamp: Long = 0L // For debouncing rapid connection changes
) : ViewState {

    /**
     * Checks if we should show offline fallback content
     */
    fun shouldShowOfflineContent(): Boolean {
        return !isOnline && cachedUsers.isNotEmpty()
    }

    /**
     * Checks if we should show a complete network error (no content at all)
     */
    fun shouldShowNetworkError(): Boolean {
        return !isOnline && cachedUsers.isEmpty()
    }

    /**
     * Checks if this is a fresh connection loss (for UX effects)
     */
    fun isConnectionJustLost(): Boolean {
        return !isOnline && lastOnlineState
    }

    /**
     * Checks if connection was just restored (for UX effects)
     */
    fun isConnectionJustRestored(): Boolean {
        return isOnline && !lastOnlineState
    }
}