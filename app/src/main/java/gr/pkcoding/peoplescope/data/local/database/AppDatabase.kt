package gr.pkcoding.peoplescope.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.utils.Constants

@Database(
    entities = [BookmarkedUserEntity::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = Constants.DATABASE_NAME
    }
}