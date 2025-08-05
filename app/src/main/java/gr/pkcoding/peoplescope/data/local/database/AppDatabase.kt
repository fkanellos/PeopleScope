package gr.pkcoding.peoplescope.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity

@Database(
    entities = [BookmarkedUserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "peoplescope_database"
    }
}