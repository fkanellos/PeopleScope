package gr.pkcoding.peoplescope.domain.model

import androidx.compose.runtime.Stable

@Stable
data class User(
    val id: String,
    val gender: String,
    val name: Name,
    val email: String,
    val phone: String,
    val cell: String,
    val picture: Picture,
    val location: Location,
    val dob: DateOfBirth,
    val nationality: String,
    val isBookmarked: Boolean = false
)
@Stable
data class Name(
    val title: String,
    val first: String,
    val last: String
) {
    fun getFullName(): String = "$title $first $last".trim()
}

@Stable
data class Picture(
    val large: String,
    val medium: String,
    val thumbnail: String
)
@Stable
data class Location(
    val street: Street,
    val city: String,
    val state: String,
    val country: String,
    val postcode: String,
    val coordinates: Coordinates,
    val timezone: Timezone
) {
    fun getFullAddress(): String {
        return "${street.number} ${street.name}, $city, $state $postcode, $country"
    }
}

data class Street(
    val number: Int,
    val name: String
)

data class Coordinates(
    val latitude: String,
    val longitude: String
)

data class Timezone(
    val offset: String,
    val description: String
)

data class DateOfBirth(
    val date: String,
    val age: Int
)