package gr.pkcoding.peoplescope.data.repository

import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.*
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserRepositoryImpl(
    private val api: RandomUserApi,
    private val bookmarkDao: BookmarkDao
) : UserRepository {

    override suspend fun getUsers(page: Int, pageSize: Int): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getUsers(page = page, results = pageSize)
                val users = response.results.toDomainModels()

                // Check bookmark status for each user
                val bookmarkedUserIds = bookmarkDao.getAllBookmarkedUsers()
                    .map { entities -> entities.map { it.id } }

                val usersWithBookmarkStatus = users.map { user ->
                    user.copy(isBookmarked = bookmarkedUserIds.map { it.contains(user.id) }.toString().toBoolean())
                }

                Result.success(users)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching users")
                Result.failure(e)
            }
        }
    }

    override suspend fun getUserById(userId: String): Result<User?> {
        return withContext(Dispatchers.IO) {
            try {
                // First check if user is bookmarked
                val bookmarkedUser = bookmarkDao.getBookmarkedUserById(userId)
                if (bookmarkedUser != null) {
                    Result.success(bookmarkedUser.toDomainModel())
                } else {
                    // If not bookmarked, we can't fetch a single user from the API
                    // The Random User API doesn't support fetching by ID
                    Result.success(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting user by ID")
                Result.failure(e)
            }
        }
    }

    override fun getBookmarkedUsers(): Flow<List<User>> {
        return bookmarkDao.getAllBookmarkedUsers()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    override fun isUserBookmarked(userId: String): Flow<Boolean> {
        return bookmarkDao.isUserBookmarked(userId)
    }

    override suspend fun toggleBookmark(user: User): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val isBookmarked = bookmarkDao.getBookmarkedUserById(user.id) != null
                if (isBookmarked) {
                    removeBookmark(user.id)
                } else {
                    bookmarkUser(user)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling bookmark")
                Result.failure(e)
            }
        }
    }

    override suspend fun bookmarkUser(user: User): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.insertBookmarkedUser(user.toBookmarkedEntity())
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error bookmarking user")
                Result.failure(e)
            }
        }
    }

    override suspend fun removeBookmark(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.deleteBookmarkedUserById(userId)
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error removing bookmark")
                Result.failure(e)
            }
        }
    }
}