package gr.pkcoding.peoplescope.domain.repository

import androidx.paging.PagingData
import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.model.UserError
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Fetch users from API
     */
    suspend fun getUsers(page: Int, pageSize: Int): Result<List<User>, DataError>

    /**
     * Get user by ID (first check bookmarks, then fetch from API if needed)
     */
    suspend fun getUserById(userId: String): Result<User, UserError>



    /**
     * Check if user is bookmarked
     */
    fun isUserBookmarked(userId: String): Flow<Boolean>

    /**
     * Toggle bookmark status for a user
     */
    suspend fun toggleBookmark(user: User): Result<Unit, DataError.Local>

    /**
     * Add user to bookmarks
     */
    suspend fun bookmarkUser(user: User): Result<Unit, DataError.Local>

    /**
     * Remove user from bookmarks
     */
    suspend fun removeBookmark(userId: String): Result<Unit, DataError.Local>

    fun getUsersPaged(): Flow<PagingData<User>>
}