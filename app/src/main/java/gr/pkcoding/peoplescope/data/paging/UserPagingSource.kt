package gr.pkcoding.peoplescope.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.mapper.toDomainModel
import gr.pkcoding.peoplescope.data.mapper.toDomainModels
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.utils.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.TimeoutException

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
            withTimeout(Constants.API_TIMEOUT) {
                val response = api.getUsers(page = page, results = params.loadSize)

                // Efficient mapping and validation
                val users = response.results
                    .asSequence()
                    .mapNotNull { it.toDomainModel() }
                    .filter { it.isValid() }
                    .toList()

                // Batch bookmark status check
                val bookmarkedIds = try {
                    bookmarkDao.getBookmarkedUserIds().toSet()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to get bookmarked IDs")
                    emptySet()
                }

                val usersWithBookmarks = users.map { user ->
                    val isBookmarked = user.id?.let { it in bookmarkedIds } ?: false
                    user.copy(isBookmarked = isBookmarked)
                }

                LoadResult.Page(
                    data = usersWithBookmarks,
                    prevKey = if (page == Constants.INITIAL_PAGE) null else page - 1,
                    nextKey = if (usersWithBookmarks.isEmpty()) null else page + 1
                )
            }
        } catch (e: TimeoutException) {
            LoadResult.Error(e)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}