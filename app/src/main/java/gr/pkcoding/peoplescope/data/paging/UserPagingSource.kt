package gr.pkcoding.peoplescope.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.toDomainModel
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.TimeoutException

class UserPagingSource(
    private val api: RandomUserApi,
    private val bookmarkDao: BookmarkDao,
    private val networkProvider: NetworkConnectivityProvider
) : PagingSource<Int, User>() {

    private suspend fun loadOfflineBookmarkedUsers(): List<User> {
        return try {
            Timber.d("📱 Loading offline bookmarked users for paging")

            val bookmarkedUsers = bookmarkDao.getAllBookmarkedUsers()
                .first()
                .map { it.toDomainModel() }
                .filter { it.isValid() }

            Timber.d("✅ Loaded ${bookmarkedUsers.size} offline users")
            bookmarkedUsers
        } catch (e: Exception) {
            Timber.e(e, "❌ Error loading offline bookmarked users")
            emptyList()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, User>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, User> {
        val page = params.key ?: Constants.INITIAL_PAGE
        val loadSize = params.loadSize

        Timber.d("📄 load() start → page=$page, loadSize=$loadSize")

        val startTime = System.currentTimeMillis()

        if (!networkProvider.isNetworkAvailable()) {
            Timber.w("📵 No network - loading offline bookmarked users")

            return try {
                if (page == Constants.INITIAL_PAGE) {
                    val offlineUsers = loadOfflineBookmarkedUsers()
                    val elapsed = System.currentTimeMillis() - startTime
                    Timber.d("⏳ Offline load complete in ${elapsed}ms, items=${offlineUsers.size}")

                    LoadResult.Page(
                        data = offlineUsers,
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    Timber.d("🔚 No more pages in offline mode")
                    LoadResult.Page(
                        data = emptyList(),
                        prevKey = if (page == Constants.INITIAL_PAGE) null else page - 1,
                        nextKey = null
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "💥 Error loading offline data")
                LoadResult.Error(e)
            }
        }

        return try {
            withTimeout(Constants.API_TIMEOUT) {
                Timber.d("🌐 Fetching page=$page from API")

                val response = api.getUsers(page = page, results = loadSize)

                val users = response.results
                    .asSequence()
                    .mapNotNull { it.toDomainModel() }
                    .filter { it.isValid() }
                    .toList()

                val bookmarkedIds = try {
                    bookmarkDao.getBookmarkedUserIds().toSet()
                } catch (e: Exception) {
                    Timber.w(e, "⚠️ Failed to get bookmarked IDs")
                    emptySet()
                }

                val usersWithBookmarks = users.map { user ->
                    val isBookmarked = user.id?.let { it in bookmarkedIds } ?: false
                    user.copy(isBookmarked = isBookmarked)
                }

                val elapsed = System.currentTimeMillis() - startTime
                Timber.d("✅ Page=$page loaded in ${elapsed}ms, items=${usersWithBookmarks.size}")

                LoadResult.Page(
                    data = usersWithBookmarks,
                    prevKey = if (page == Constants.INITIAL_PAGE) null else page - 1,
                    nextKey = if (usersWithBookmarks.isEmpty()) null else page + 1
                )
            }
        } catch (e: TimeoutException) {
            Timber.e("⏱️ Timeout loading page=$page after ${System.currentTimeMillis() - startTime}ms")
            handleNetworkErrorWithFallback(e, page)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error loading page=$page after ${System.currentTimeMillis() - startTime}ms")
            handleNetworkErrorWithFallback(e, page)
        }
    }

    private suspend fun handleNetworkErrorWithFallback(
        networkError: Exception,
        page: Int
    ): LoadResult<Int, User> {
        // Fallback to offline data only for first page
        if (page == 1) {
            return try {
                val offlineUsers = loadOfflineBookmarkedUsers()
                Timber.d("🔄 Network error fallback: returning ${offlineUsers.size} offline users")

                LoadResult.Page(
                    data = offlineUsers,
                    prevKey = null,
                    nextKey = null // Don't try to load more pages after network error
                )
            } catch (offlineError: Exception) {
                Timber.e(offlineError, "Offline fallback also failed")
                LoadResult.Error(networkError)
            }
        } else {
            return LoadResult.Error(networkError)
        }
    }
}
