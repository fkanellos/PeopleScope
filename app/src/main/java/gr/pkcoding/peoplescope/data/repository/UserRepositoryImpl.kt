package gr.pkcoding.peoplescope.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.*
import gr.pkcoding.peoplescope.data.paging.UserPagingSource
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import gr.pkcoding.peoplescope.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserRepositoryImpl(
    private val api: RandomUserApi,
    private val bookmarkDao: BookmarkDao
) : UserRepository {

    override suspend fun getUsers(page: Int, pageSize: Int): Result<List<User>, DataError> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Making API call to getUsers(page=$page, pageSize=$pageSize)")

                val response = api.getUsers(page = page, results = pageSize)
                Timber.d("API response received: ${response.results.size} users")

                val users = response.results.toDomainModels()
                Timber.d("Mapped to domain: ${users.size} users")

                // Update bookmark status for each user
                val usersWithBookmarkStatus = users.map { user ->
                    val isBookmarked = try {
                        bookmarkDao.getBookmarkedUserById(user.id) != null
                    } catch (e: Exception) {
                        Timber.w(e, "Error checking bookmark status for user ${user.id}")
                        false
                    }
                    user.copy(isBookmarked = isBookmarked)
                }

                Timber.d("Final result: ${usersWithBookmarkStatus.size} users with bookmark status")
                Result.Success(usersWithBookmarkStatus)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching users from API")
                Result.Error(DataError.Network(e.toNetworkError()))
            }
        }
    }

    override suspend fun getUserById(userId: String): Result<User, UserError> {
        return withContext(Dispatchers.IO) {
            try {
                // First check if user is bookmarked
                val bookmarkedUser = bookmarkDao.getBookmarkedUserById(userId)
                if (bookmarkedUser != null) {
                    Result.Success(bookmarkedUser.toDomainModel())
                } else {
                    // If not bookmarked, we can't fetch a single user from the API
                    // The Random User API doesn't support fetching by ID
                    Result.Error(UserError.UserNotFound(userId))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting user by ID")
                Result.Error(UserError.UserNotFound(userId))
            }
        }
    }

    override fun getBookmarkedUsers(): Flow<Result<List<User>, DataError.Local>> {
        return bookmarkDao.getAllBookmarkedUsers()
            .map { entities ->
                Result.Success(entities.map { it.toDomainModel() }) as Result<List<User>, DataError.Local>
            }
            .catch { e ->
                Timber.e(e, "Error getting bookmarked users")
                emit(Result.Error(DataError.Local(e.toLocalError())))
            }
            .flowOn(Dispatchers.IO)
    }

    override fun isUserBookmarked(userId: String): Flow<Boolean> {
        return bookmarkDao.isUserBookmarked(userId)
            .flowOn(Dispatchers.IO)
    }

    override suspend fun toggleBookmark(user: User): Result<Unit, DataError.Local> {
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
                Result.Error(DataError.Local(e.toLocalError()))
            }
        }
    }

    override suspend fun bookmarkUser(user: User): Result<Unit, DataError.Local> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.insertBookmarkedUser(user.toBookmarkedEntity())
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error bookmarking user")
                Result.Error(DataError.Local(e.toLocalError()))
            }
        }
    }

    override suspend fun removeBookmark(userId: String): Result<Unit, DataError.Local> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.deleteBookmarkedUserById(userId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error removing bookmark")
                Result.Error(DataError.Local(e.toLocalError()))
            }
        }
    }

    override fun getUsersPaged(): Flow<PagingData<User>> {
        Timber.d("🚀 Creating paged users flow")

        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = Constants.PAGE_SIZE,
                prefetchDistance = 1, // Reduced from 3 to minimize background loading
                maxSize = Constants.PAGE_SIZE * 10 // Limit memory usage
            ),
            pagingSourceFactory = {
                Timber.d("🔥 Creating new UserPagingSource")
                UserPagingSource(api = api, bookmarkDao = bookmarkDao)
            }
        ).flow.flowOn(Dispatchers.IO)
    }

    // Debug method - test direct API call
    suspend fun testApiCall(): String {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Testing direct API call...")
                val response = api.getUsers(page = 1, results = 5)
                "SUCCESS: Got ${response.results.size} users. First user: ${response.results.firstOrNull()?.name?.first}"
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
        }
    }
}