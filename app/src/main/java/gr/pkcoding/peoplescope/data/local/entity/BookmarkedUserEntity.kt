package gr.pkcoding.peoplescope.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_users")
data class BookmarkedUserEntity(
    @PrimaryKey
    val id: String,
    val gender: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val cell: String,
    val pictureLarge: String,
    val pictureMedium: String,
    val pictureThumbnail: String,
    val streetNumber: Int,
    val streetName: String,
    val city: String,
    val state: String,
    val country: String,
    val postcode: String,
    val latitude: String,
    val longitude: String,
    val timezoneOffset: String,
    val timezoneDescription: String,
    val dobDate: String,
    val dobAge: Int,
    val nationality: String,
    val bookmarkedAt: Long
)