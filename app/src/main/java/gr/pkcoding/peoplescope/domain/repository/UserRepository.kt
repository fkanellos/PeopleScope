package gr.pkcoding.peoplescope.domain.repository

import gr.pkcoding.peoplescope.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Fetch users from API
     */
    suspend fun getUsers(page: Int, pageSize: Int): Result<List<User>>

    /**
     * Get user by ID (first check bookmarks, then fetch from API if needed)
     */
    suspend fun getUserById(userId: String): Result<User?>

    /**
     * Get all bookmarked users
     */
    fun getBookmarkedUsers(): Flow<List<User>>

    /**
     * Check if user is bookmarked
     */
    fun isUserBookmarked(userId: String): Flow<Boolean>

    /**
     * Toggle bookmark status for a user
     */
    suspend fun toggleBookmark(user: User): Result<Unit>

    /**
     * Add user to bookmarks
     */
    suspend fun bookmarkUser(user: User): Result<Unit>

    /**
     * Remove user from bookmarks
     */
    suspend fun removeBookmark(userId: String): Result<Unit>
}