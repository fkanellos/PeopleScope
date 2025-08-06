package gr.pkcoding.peoplescope.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.toDomainModels
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.utils.Constants
import kotlinx.coroutines.flow.first
import timber.log.Timber

class UserPagingSource(
    private val api: RandomUserApi,
    private val bookmarkDao: BookmarkDao
) : PagingSource<Int, User>() {

    override fun getRefreshKey(state: PagingState<Int, User>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, User> {
        val page = params.key ?: Constants.INITIAL_PAGE

        Timber.d("🔄 Loading page: $page, loadSize: ${params.loadSize}")

        return try {
            // Make API call
            val response = api.getUsers(
                page = page,
                results = params.loadSize
            )

            Timber.d("✅ API response: ${response.results.size} users received")

            // Map to domain models
            val users = response.results.toDomainModels()

            Timber.d("✅ Mapped users: ${users.size} users after mapping")

            if (users.isEmpty()) {
                Timber.w("⚠️ No users after mapping! Check UserDto.toDomainModel()")
            }

            // Get bookmarked user IDs - but don't use .first() to avoid blocking
            val bookmarkedUserIds = try {
                // Use a simple suspend call instead of collecting Flow
                val bookmarkedUsers = bookmarkDao.getAllBookmarkedUsers().first()
                bookmarkedUsers.map { it.id }.toSet()
            } catch (e: Exception) {
                Timber.w(e, "Failed to get bookmarked users, using empty set")
                emptySet()
            }

            // Update bookmark status for each user
            val usersWithBookmarkStatus = users.map { user ->
                user.copy(isBookmarked = user.id in bookmarkedUserIds)
            }

            LoadResult.Page(
                data = usersWithBookmarkStatus,
                prevKey = if (page == Constants.INITIAL_PAGE) null else page - 1,
                nextKey = if (usersWithBookmarkStatus.isEmpty()) null else page + 1
            ).also {
                Timber.d("✅ Returning ${usersWithBookmarkStatus.size} users for page $page")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error loading users page: $page")
            LoadResult.Error(e)
        }
    }
}