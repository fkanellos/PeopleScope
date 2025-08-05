package gr.pkcoding.peoplescope.data.local.dao

import androidx.room.*
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarked_users ORDER BY bookmarkedAt DESC")
    fun getAllBookmarkedUsers(): Flow<List<BookmarkedUserEntity>>

    @Query("SELECT * FROM bookmarked_users WHERE id = :userId")
    suspend fun getBookmarkedUserById(userId: String): BookmarkedUserEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_users WHERE id = :userId)")
    fun isUserBookmarked(userId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarkedUser(user: BookmarkedUserEntity)

    @Delete
    suspend fun deleteBookmarkedUser(user: BookmarkedUserEntity)

    @Query("DELETE FROM bookmarked_users WHERE id = :userId")
    suspend fun deleteBookmarkedUserById(userId: String)
}