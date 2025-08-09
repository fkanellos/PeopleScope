package gr.pkcoding.peoplescope.data.mapper

import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.remote.dto.*
import gr.pkcoding.peoplescope.domain.model.*
import timber.log.Timber

/**
 * Maps UserDto from API to domain User model
 */
fun UserDto.toDomainModel(): User? {
    return try {
        // Essential validation - skip users without required data
        val userId = login?.uuid?.takeIf { it.isNotBlank() }
        val firstName = name?.first?.takeIf { it.isNotBlank() }
        val lastName = name?.last?.takeIf { it.isNotBlank() }

        if (userId == null || firstName == null || lastName == null) {
            Timber.w("Skipping user with missing essential data: UUID=$userId, name=$firstName $lastName")
            return null
        }

        User(
            id = userId,
            gender = gender?.takeIf { it.isNotBlank() },
            name = Name(
                title = name.title?.takeIf { it.isNotBlank() },
                first = firstName,
                last = lastName
            ),
            email = email?.takeIf { it.isNotBlank() },
            phone = phone?.takeIf { it.isNotBlank() },
            cell = cell?.takeIf { it.isNotBlank() },
            picture = picture?.let {
                Picture(
                    large = it.large?.takeIf { url -> url.isNotBlank() },
                    medium = it.medium?.takeIf { url -> url.isNotBlank() },
                    thumbnail = it.thumbnail?.takeIf { url -> url.isNotBlank() }
                )
            },
            location = location?.let { loc ->
                Location(
                    street = loc.street?.let { st ->
                        Street(
                            number = st.number,
                            name = st.name?.takeIf { it.isNotBlank() }
                        )
                    },
                    city = loc.city?.takeIf { it.isNotBlank() },
                    state = loc.state?.takeIf { it.isNotBlank() },
                    country = loc.country?.takeIf { it.isNotBlank() },
                    postcode = loc.postcode?.toString()?.takeIf { it.isNotBlank() },
                    coordinates = loc.coordinates?.let { coord ->
                        Coordinates(
                            latitude = coord.latitude?.takeIf { it.isNotBlank() },
                            longitude = coord.longitude?.takeIf { it.isNotBlank() }
                        )
                    },
                    timezone = loc.timezone?.let { tz ->
                        Timezone(
                            offset = tz.offset?.takeIf { it.isNotBlank() },
                            description = tz.description?.takeIf { it.isNotBlank() }
                        )
                    }
                )
            },
            dob = dob?.let { dateOfBirth ->
                DateOfBirth(
                    date = dateOfBirth.date?.takeIf { it.isNotBlank() },
                    age = dateOfBirth.age?.takeIf { it > 0 }
                )
            },
            nationality = nat?.takeIf { it.isNotBlank() },
            isBookmarked = false
        )
    } catch (e: Exception) {
        Timber.e(e, "Error mapping UserDto to User")
        null
    }
}

/**
 * Maps list of UserDto to list of domain User models
 */
//todo delete?
fun List<UserDto>.toDomainModels(): List<User> {
    Timber.d("Mapping ${this.size} UserDto objects")

    val users = this.mapNotNull { userDto ->
        userDto.toDomainModel()
    }

    Timber.d("Successfully mapped ${users.size} out of ${this.size} users")
    return users
}

/**
 * Maps domain User model to BookmarkedUserEntity for Room database
 */
fun User.toBookmarkedEntity(): BookmarkedUserEntity? {
    val safeId = id?.takeIf { it.isNotBlank() }
    val safeName = name

    if (safeId == null || safeName?.first.isNullOrBlank() || safeName.last.isNullOrBlank()) {
        Timber.w("Cannot create BookmarkedUserEntity: missing required fields")
        return null
    }

    return try {
        BookmarkedUserEntity(
            id = safeId,
            gender = gender ?: "unknown",
            title = safeName.title ?: "",
            firstName = safeName.first,
            lastName = safeName.last,
            email = email ?: "",
            phone = phone ?: "",
            cell = cell ?: "",
            pictureLarge = picture?.large ?: "",
            pictureMedium = picture?.medium ?: "",
            pictureThumbnail = picture?.thumbnail ?: "",
            streetNumber = location?.street?.number ?: 0,
            streetName = location?.street?.name ?: "",
            city = location?.city ?: "",
            state = location?.state ?: "",
            country = location?.country ?: "",
            postcode = location?.postcode ?: "",
            latitude = location?.coordinates?.latitude ?: "",
            longitude = location?.coordinates?.longitude ?: "",
            timezoneOffset = location?.timezone?.offset ?: "",
            timezoneDescription = location?.timezone?.description ?: "",
            dobDate = dob?.date ?: "",
            dobAge = dob?.age ?: 0,
            nationality = nationality ?: "",
            bookmarkedAt = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        Timber.e(e, "Error creating BookmarkedUserEntity")
        null
    }
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