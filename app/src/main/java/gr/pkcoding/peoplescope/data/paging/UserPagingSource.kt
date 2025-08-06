package gr.pkcoding.peoplescope.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.toDomainModels
import gr.pkcoding.peoplescope.data.mapper.toNetworkError
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.NetworkError
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

        return try {
            val response = api.getUsers(
                page = page,
                results = params.loadSize
            )

            val users = response.results.toDomainModels()

            // Get bookmarked user IDs
            val bookmarkedUserIds = bookmarkDao.getAllBookmarkedUsers()
                .first()
                .map { it.id }
                .toSet()

            // Update bookmark status for each user
            val usersWithBookmarkStatus = users.map { user ->
                user.copy(isBookmarked = user.id in bookmarkedUserIds)
            }

            LoadResult.Page(
                data = usersWithBookmarkStatus,
                prevKey = if (page == Constants.INITIAL_PAGE) null else page - 1,
                nextKey = if (users.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading users page: $page")
            LoadResult.Error(e)
        }
    }
}