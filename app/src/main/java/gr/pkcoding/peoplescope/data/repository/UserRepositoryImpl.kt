package gr.pkcoding.peoplescope.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.*
import gr.pkcoding.peoplescope.data.paging.UserPagingSource
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import gr.pkcoding.peoplescope.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class UserRepositoryImpl(
    private val api: RandomUserApi,
    private val bookmarkDao: BookmarkDao
) : UserRepository {

    private val userCache = mutableMapOf<String, User>()

    @Volatile
    private var bookmarkIdsCache: Pair<Set<String>, Long>? = null
    private val bookmarkCacheTtl = 30_000L // 30 seconds

    private suspend fun getBookmarkedUserIds(): Set<String> {
        val now = System.currentTimeMillis()
        val cached = bookmarkIdsCache

        // Return cached if still valid
        if (cached != null && (now - cached.second) < bookmarkCacheTtl) {
            return cached.first
        }

        // Fetch fresh data
        return try {
            val ids = bookmarkDao.getBookmarkedUserIds().toSet()
            bookmarkIdsCache = ids to now
            ids
        } catch (e: Exception) {
            Timber.w(e, "Failed to get bookmarked user IDs")
            cached?.first ?: emptySet()
        }
    }

    override suspend fun getUsers(page: Int, pageSize: Int): Result<List<User>, DataError> {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(Constants.API_TIMEOUT) {
                    Timber.d("🔄 API call: page=$page, size=$pageSize")

                    val response = api.getUsers(page = page, results = pageSize)

                    // ✅ Efficient filtering και mapping
                    val users = response.results
                        .asSequence() // Lazy evaluation
                        .mapNotNull { it.toDomainModel() }
                        .filter { it.isValid() }
                        .toList()

                    if (users.isEmpty() && response.results.isNotEmpty()) {
                        Timber.w("⚠️ All users filtered out - bad API data")
                    }

                    // ✅ Efficient bookmark status update
                    val bookmarkedIds = getBookmarkedUserIds()
                    val usersWithBookmarks = users.map { user ->
                        val isBookmarked = user.id?.let { it in bookmarkedIds } ?: false
                        user.copy(isBookmarked = isBookmarked).also { finalUser ->
                            // Cache για navigation
                            finalUser.id?.let { userCache[it] = finalUser }
                        }
                    }

                    Timber.d("✅ Returned ${usersWithBookmarks.size} valid users")
                    Result.Success(usersWithBookmarks)
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("⏱️ API timeout for page $page")
                Result.Error(DataError.Network(NetworkError.REQUEST_TIMEOUT))
            } catch (e: Exception) {
                Timber.e(e, "❌ API error for page $page")
                Result.Error(DataError.Network(e.toNetworkError()))
            }
        }
    }

    override suspend fun getUserById(userId: String): Result<User, UserError> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("🔍 Getting user by ID: $userId")

                // Check cache first με fresh bookmark status
                userCache[userId]?.let { cachedUser ->
                    Timber.d("✅ Found user in cache: ${cachedUser.getDisplayName()}")
                    val isBookmarked = try {
                        bookmarkDao.getBookmarkedUserById(userId) != null
                    } catch (e: Exception) {
                        Timber.w(e, "Error checking bookmark status")
                        false
                    }
                    return@withContext Result.Success(
                        cachedUser.copy(isBookmarked = isBookmarked)
                    )
                }

                // Check bookmarks database
                val bookmarkedUser = try {
                    bookmarkDao.getBookmarkedUserById(userId)
                } catch (e: Exception) {
                    Timber.w(e, "Error querying bookmarked user")
                    null
                }

                if (bookmarkedUser != null) {
                    Timber.d("✅ Found bookmarked user: ${bookmarkedUser.firstName}")
                    val user = bookmarkedUser.toDomainModel()
                    userCache[userId] = user // Cache για future
                    Result.Success(user)
                } else {
                    Timber.w("❌ User $userId not found")
                    Result.Error(UserError.UserNotFound(userId))
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error getting user by ID")
                Result.Error(UserError.UserNotFound(userId))
            }
        }
    }

    override fun isUserBookmarked(userId: String): Flow<Boolean> {
        return bookmarkDao.isUserBookmarked(userId)
            .flowOn(Dispatchers.IO)
    }

    override suspend fun toggleBookmark(user: User): Result<Unit, DataError.Local> {
        if (!user.isValid() || user.id.isNullOrBlank()) {
            return Result.Error(DataError.Local(LocalError.DATABASE_ERROR))
        }

        return withContext(Dispatchers.IO) {
            try {
                val isBookmarked = bookmarkDao.getBookmarkedUserById(user.id) != null
                Timber.d("🔄 Toggle bookmark for ${user.getDisplayName()}: $isBookmarked")

                val result = if (isBookmarked) {
                    removeBookmark(user.id)
                } else {
                    bookmarkUser(user)
                }

                // Invalidate cache after change
                if (result.isSuccess()) {
                    bookmarkIdsCache = null
                }

                result
            } catch (e: Exception) {
                Timber.e(e, "❌ Error toggling bookmark")
                Result.Error(DataError.Local(e.toLocalError()))
            }
        }
    }

    override suspend fun bookmarkUser(user: User): Result<Unit, DataError.Local> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = user.toBookmarkedEntity()
                    ?: return@withContext Result.Error(DataError.Local(LocalError.DATABASE_ERROR))

                bookmarkDao.insertBookmarkedUser(entity)
                Timber.d("✅ Bookmarked user: ${user.getDisplayName()}")
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error bookmarking user")
                Result.Error(DataError.Local(e.toLocalError()))
            }
        }
    }

    override suspend fun removeBookmark(userId: String): Result<Unit, DataError.Local> {
        return withContext(Dispatchers.IO) {
            try {
                bookmarkDao.deleteBookmarkedUserById(userId)
                Timber.d("✅ Removed bookmark: $userId")
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "❌ Error removing bookmark")
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
                prefetchDistance = 3,
                maxSize = Constants.PAGE_SIZE * 20,
                jumpThreshold = Int.MIN_VALUE
            ),
            pagingSourceFactory = {
                UserPagingSource(api, bookmarkDao)
            }
        ).flow
            .map { pagingData ->
                pagingData.map { user ->
                    // Cache για navigation
                    if (user.isValid() && !user.id.isNullOrBlank()) {
                        userCache[user.id] = user
                    }
                    user
                }
            }
            .flowOn(Dispatchers.IO)
    }
}