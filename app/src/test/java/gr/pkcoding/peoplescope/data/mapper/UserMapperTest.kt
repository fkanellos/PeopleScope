package gr.pkcoding.peoplescope.data.mapper

import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.remote.dto.CoordinatesDto
import gr.pkcoding.peoplescope.data.remote.dto.DobDto
import gr.pkcoding.peoplescope.data.remote.dto.IdDto
import gr.pkcoding.peoplescope.data.remote.dto.LocationDto
import gr.pkcoding.peoplescope.data.remote.dto.LoginDto
import gr.pkcoding.peoplescope.data.remote.dto.NameDto
import gr.pkcoding.peoplescope.data.remote.dto.PictureDto
import gr.pkcoding.peoplescope.data.remote.dto.RegisteredDto
import gr.pkcoding.peoplescope.data.remote.dto.StreetDto
import gr.pkcoding.peoplescope.data.remote.dto.TimezoneDto
import gr.pkcoding.peoplescope.data.remote.dto.UserDto
import gr.pkcoding.peoplescope.domain.model.Coordinates
import gr.pkcoding.peoplescope.domain.model.DateOfBirth
import gr.pkcoding.peoplescope.domain.model.Location
import gr.pkcoding.peoplescope.domain.model.Name
import gr.pkcoding.peoplescope.domain.model.Picture
import gr.pkcoding.peoplescope.domain.model.Street
import gr.pkcoding.peoplescope.domain.model.Timezone
import gr.pkcoding.peoplescope.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UserMapperTest {

    private val validUserDto = UserDto(
        gender = "male",
        name = NameDto("Mr", "John", "Doe"),
        location = LocationDto(
            street = StreetDto(123, "Main St"),
            city = "New York",
            state = "NY",
            country = "USA",
            postcode = "10001",
            coordinates = CoordinatesDto("40.7128", "-74.0060"),
            timezone = TimezoneDto("-5:00", "Eastern Time")
        ),
        email = "john.doe@example.com",
        login = LoginDto(
            uuid = "test-uuid",
            username = "johndoe123",
            password = "password123",
            salt = "salt123",
            md5 = "md5hash",
            sha1 = "sha1hash",
            sha256 = "sha256hash"
        ),
        dob = DobDto("1990-01-01T00:00:00.000Z", 33),
        registered = RegisteredDto("2020-01-01T00:00:00.000Z", 5),
        phone = "+1234567890",
        cell = "+0987654321",
        id = IdDto("SSN", "123-45-6789"),
        picture = PictureDto("large.jpg", "medium.jpg", "thumbnail.jpg"),
        nat = "US"
    )

    @Test
    fun `UserDto toDomainModel should map all fields correctly`() {
        // When
        val user = validUserDto.toDomainModel()

        // Then
        assertNotNull("User should not be null", user)
        assertEquals("test-uuid", user!!.id)
        assertEquals("male", user.gender)
        assertEquals("Mr", user.name?.title)
        assertEquals("John", user.name?.first)
        assertEquals("Doe", user.name?.last)
        assertEquals("john.doe@example.com", user.email)
        assertEquals("+1234567890", user.phone)
        assertEquals("+0987654321", user.cell)
        assertEquals("large.jpg", user.picture?.large)
        assertEquals("medium.jpg", user.picture?.medium)
        assertEquals("thumbnail.jpg", user.picture?.thumbnail)
        assertEquals(123, user.location?.street?.number)
        assertEquals("Main St", user.location?.street?.name)
        assertEquals("New York", user.location?.city)
        assertEquals("NY", user.location?.state)
        assertEquals("USA", user.location?.country)
        assertEquals("10001", user.location?.postcode)
        assertEquals("40.7128", user.location?.coordinates?.latitude)
        assertEquals("-74.0060", user.location?.coordinates?.longitude)
        assertEquals("-5:00", user.location?.timezone?.offset)
        assertEquals("Eastern Time", user.location?.timezone?.description)
        assertEquals("1990-01-01T00:00:00.000Z", user.dob?.date)
        assertEquals(33, user.dob?.age)
        assertEquals("US", user.nationality)
        assertFalse("New user should not be bookmarked", user.isBookmarked)
    }

    @Test
    fun `UserDto toDomainModel should return null for missing uuid`() {
        // Given
        val userDtoWithoutUuid = validUserDto.copy(
            login = LoginDto(null, "username", "password", "salt", "md5", "sha1", "sha256")
        )

        // When
        val user = userDtoWithoutUuid.toDomainModel()

        // Then
        assertNull("User without UUID should be null", user)
    }

    @Test
    fun `UserDto toDomainModel should return null for blank uuid`() {
        // Given
        val userDtoWithBlankUuid = validUserDto.copy(
            login = LoginDto("   ", "username", "password", "salt", "md5", "sha1", "sha256")
        )

        // When
        val user = userDtoWithBlankUuid.toDomainModel()

        // Then
        assertNull("User with blank UUID should be null", user)
    }

    @Test
    fun `UserDto toDomainModel should return null for missing first name`() {
        // Given
        val userDtoWithoutFirstName = validUserDto.copy(
            name = NameDto("Mr", null, "Doe")
        )

        // When
        val user = userDtoWithoutFirstName.toDomainModel()

        // Then
        assertNull("User without first name should be null", user)
    }

    @Test
    fun `UserDto toDomainModel should return null for missing last name`() {
        // Given
        val userDtoWithoutLastName = validUserDto.copy(
            name = NameDto("Mr", "John", null)
        )

        // When
        val user = userDtoWithoutLastName.toDomainModel()

        // Then
        assertNull("User without last name should be null", user)
    }

    @Test
    fun `UserDto toDomainModel should handle null location gracefully`() {
        // Given
        val userDtoWithoutLocation = validUserDto.copy(location = null)

        // When
        val user = userDtoWithoutLocation.toDomainModel()

        // Then
        assertNotNull("User should not be null", user)
        assertNull("Location should be null", user!!.location)
    }

    @Test
    fun `UserDto toDomainModel should handle null picture gracefully`() {
        // Given
        val userDtoWithoutPicture = validUserDto.copy(picture = null)

        // When
        val user = userDtoWithoutPicture.toDomainModel()

        // Then
        assertNotNull("User should not be null", user)
        assertNull("Picture should be null", user!!.picture)
    }

    @Test
    fun `UserDto toDomainModel should handle blank email gracefully`() {
        // Given
        val userDtoWithBlankEmail = validUserDto.copy(email = "   ")

        // When
        val user = userDtoWithBlankEmail.toDomainModel()

        // Then
        assertNotNull("User should not be null", user)
        assertNull("Blank email should be null", user!!.email)
    }

    @Test
    fun `User toBookmarkedEntity should map all fields correctly`() {
        // Given
        val user = User(
            id = "test-id",
            gender = "female",
            name = Name("Ms", "Jane", "Smith"),
            email = "jane.smith@example.com",
            phone = "+1987654321",
            cell = "+1234567890",
            picture = Picture("large2.jpg", "medium2.jpg", "thumbnail2.jpg"),
            location = Location(
                street = Street(456, "Oak Ave"),
                city = "Los Angeles",
                state = "CA",
                country = "USA",
                postcode = "90210",
                coordinates = Coordinates("34.0522", "-118.2437"),
                timezone = Timezone("-8:00", "Pacific Time")
            ),
            dob = DateOfBirth("1992-05-15T00:00:00.000Z", 31),
            nationality = "US",
            isBookmarked = true
        )

        // When
        val entity = user.toBookmarkedEntity()

        // Then
        assertNotNull("Entity should not be null", entity)
        assertEquals("test-id", entity!!.id)
        assertEquals("female", entity.gender)
        assertEquals("Ms", entity.title)
        assertEquals("Jane", entity.firstName)
        assertEquals("Smith", entity.lastName)
        assertEquals("jane.smith@example.com", entity.email)
        assertEquals("+1987654321", entity.phone)
        assertEquals("+1234567890", entity.cell)
        assertEquals("large2.jpg", entity.pictureLarge)
        assertEquals("medium2.jpg", entity.pictureMedium)
        assertEquals("thumbnail2.jpg", entity.pictureThumbnail)
        assertEquals(456, entity.streetNumber)
        assertEquals("Oak Ave", entity.streetName)
        assertEquals("Los Angeles", entity.city)
        assertEquals("CA", entity.state)
        assertEquals("USA", entity.country)
        assertEquals("90210", entity.postcode)
        assertEquals("34.0522", entity.latitude)
        assertEquals("-118.2437", entity.longitude)
        assertEquals("-8:00", entity.timezoneOffset)
        assertEquals("Pacific Time", entity.timezoneDescription)
        assertEquals("1992-05-15T00:00:00.000Z", entity.dobDate)
        assertEquals(31, entity.dobAge)
        assertEquals("US", entity.nationality)
        assertTrue("Timestamp should be recent", entity.bookmarkedAt > 0)
    }

    @Test
    fun `User toBookmarkedEntity should return null for null id`() {
        // Given
        val userWithNullId = User(
            id = null,
            gender = "male",
            name = Name("Mr", "John", "Doe"),
            email = "john@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // When
        val entity = userWithNullId.toBookmarkedEntity()

        // Then
        assertNull("Entity should be null for user with null id", entity)
    }

    @Test
    fun `BookmarkedUserEntity toDomainModel should map all fields correctly`() {
        // Given
        val entity = BookmarkedUserEntity(
            id = "test-id",
            gender = "male",
            title = "Dr",
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            pictureLarge = "large.jpg",
            pictureMedium = "medium.jpg",
            pictureThumbnail = "thumbnail.jpg",
            streetNumber = 123,
            streetName = "Main St",
            city = "New York",
            state = "NY",
            country = "USA",
            postcode = "10001",
            latitude = "40.7128",
            longitude = "-74.0060",
            timezoneOffset = "-5:00",
            timezoneDescription = "Eastern Time",
            dobDate = "1990-01-01T00:00:00.000Z",
            dobAge = 33,
            nationality = "US",
            bookmarkedAt = System.currentTimeMillis()
        )

        // When
        val user = entity.toDomainModel()

        // Then
        assertEquals("test-id", user.id)
        assertEquals("male", user.gender)
        assertEquals("Dr", user.name?.title)
        assertEquals("John", user.name?.first)
        assertEquals("Doe", user.name?.last)
        assertEquals("john.doe@example.com", user.email)
        assertEquals("+1234567890", user.phone)
        assertEquals("+0987654321", user.cell)
        assertEquals("large.jpg", user.picture?.large)
        assertEquals("medium.jpg", user.picture?.medium)
        assertEquals("thumbnail.jpg", user.picture?.thumbnail)
        assertEquals(123, user.location?.street?.number)
        assertEquals("Main St", user.location?.street?.name)
        assertEquals("New York", user.location?.city)
        assertEquals("NY", user.location?.state)
        assertEquals("USA", user.location?.country)
        assertEquals("10001", user.location?.postcode)
        assertEquals("40.7128", user.location?.coordinates?.latitude)
        assertEquals("-74.0060", user.location?.coordinates?.longitude)
        assertEquals("-5:00", user.location?.timezone?.offset)
        assertEquals("Eastern Time", user.location?.timezone?.description)
        assertEquals("1990-01-01T00:00:00.000Z", user.dob?.date)
        assertEquals(33, user.dob?.age)
        assertEquals("US", user.nationality)
        assertTrue("Bookmarked user should be marked as bookmarked", user.isBookmarked)
    }

    @Test
    fun `UserDto toDomainModel should handle postcode as string`() {
        // Given
        val userDtoWithStringPostcode = validUserDto.copy(
            location = validUserDto.location?.copy(postcode = "12345")
        )

        // When
        val user = userDtoWithStringPostcode.toDomainModel()

        // Then
        assertNotNull("User should not be null", user)
        assertEquals("12345", user!!.location?.postcode)
    }

    @Test
    fun `UserDto toDomainModel should handle postcode as integer`() {
        // Given
        val userDtoWithIntPostcode = validUserDto.copy(
            location = validUserDto.location?.copy(postcode = 12345)
        )

        // When
        val user = userDtoWithIntPostcode.toDomainModel()

        // Then
        assertNotNull("User should not be null", user)
        assertEquals("12345", user!!.location?.postcode)
    }
}