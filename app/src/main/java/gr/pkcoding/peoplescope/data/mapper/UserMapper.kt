package gr.pkcoding.peoplescope.data.mapper
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.remote.dto.*
import gr.pkcoding.peoplescope.domain.model.*

/**
 * Maps UserDto from API to domain User model
 */
fun UserDto.toDomainModel(): User? {
    return try {
        User(
            id = login?.uuid ?: return null,
            gender = gender ?: "",
            name = Name(
                title = name?.title ?: "",
                first = name?.first ?: "",
                last = name?.last ?: ""
            ),
            email = email ?: "",
            phone = phone ?: "",
            cell = cell ?: "",
            picture = Picture(
                large = picture?.large ?: "",
                medium = picture?.medium ?: "",
                thumbnail = picture?.thumbnail ?: ""
            ),
            location = Location(
                street = Street(
                    number = location?.street?.number ?: 0,
                    name = location?.street?.name ?: ""
                ),
                city = location?.city ?: "",
                state = location?.state ?: "",
                country = location?.country ?: "",
                postcode = location?.postcode?.toString() ?: "",
                coordinates = Coordinates(
                    latitude = location?.coordinates?.latitude ?: "",
                    longitude = location?.coordinates?.longitude ?: ""
                ),
                timezone = Timezone(
                    offset = location?.timezone?.offset ?: "",
                    description = location?.timezone?.description ?: ""
                )
            ),
            dob = DateOfBirth(
                date = dob?.date ?: "",
                age = dob?.age ?: 0
            ),
            nationality = nat ?: "",
            isBookmarked = false
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Maps list of UserDto to list of domain User models
 */
fun List<UserDto>.toDomainModels(): List<User> {
    return this.mapNotNull { it.toDomainModel() }
}

/**
 * Maps domain User model to BookmarkedUserEntity for Room database
 */
fun User.toBookmarkedEntity(): BookmarkedUserEntity {
    return BookmarkedUserEntity(
        id = id,
        gender = gender,
        title = name.title,
        firstName = name.first,
        lastName = name.last,
        email = email,
        phone = phone,
        cell = cell,
        pictureLarge = picture.large,
        pictureMedium = picture.medium,
        pictureThumbnail = picture.thumbnail,
        streetNumber = location.street.number,
        streetName = location.street.name,
        city = location.city,
        state = location.state,
        country = location.country,
        postcode = location.postcode,
        latitude = location.coordinates.latitude,
        longitude = location.coordinates.longitude,
        timezoneOffset = location.timezone.offset,
        timezoneDescription = location.timezone.description,
        dobDate = dob.date,
        dobAge = dob.age,
        nationality = nationality,
        bookmarkedAt = System.currentTimeMillis()
    )
}

/**
 * Maps BookmarkedUserEntity from Room to domain User model
 */
fun BookmarkedUserEntity.toDomainModel(): User {
    return User(
        id = id,
        gender = gender,
        name = Name(
            title = title,
            first = firstName,
            last = lastName
        ),
        email = email,
        phone = phone,
        cell = cell,
        picture = Picture(
            large = pictureLarge,
            medium = pictureMedium,
            thumbnail = pictureThumbnail
        ),
        location = Location(
            street = Street(
                number = streetNumber,
                name = streetName
            ),
            city = city,
            state = state,
            country = country,
            postcode = postcode,
            coordinates = Coordinates(
                latitude = latitude,
                longitude = longitude
            ),
            timezone = Timezone(
                offset = timezoneOffset,
                description = timezoneDescription
            )
        ),
        dob = DateOfBirth(
            date = dobDate,
            age = dobAge
        ),
        nationality = nationality,
        isBookmarked = true
    )
}