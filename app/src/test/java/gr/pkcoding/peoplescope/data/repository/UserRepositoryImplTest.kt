package gr.pkcoding.peoplescope.data.repository

import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.data.remote.dto.*
import gr.pkcoding.peoplescope.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.UnknownHostException

@RunWith(JUnit4::class)
class UserRepositoryImplTest {

    private lateinit var api: RandomUserApi
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var repository: UserRepositoryImpl

    private val testUserDto = UserDto(
        gender = "male",
        name = NameDto("Mr", "John", "Doe"),
        email = "john.doe@example.com",
        phone = "+1234567890",
        cell = "+0987654321",
        picture = PictureDto("large.jpg", "medium.jpg", "thumbnail.jpg"),
        location = LocationDto(
            street = StreetDto(123, "Main St"),
            city = "New York",
            state = "NY",
            country = "USA",
            postcode = "10001",
            coordinates = CoordinatesDto("40.7128", "-74.0060"),
            timezone = TimezoneDto("-5:00", "Eastern Time")
        ),
        dob = DobDto("1990-01-01T00:00:00.000Z", 33),
        login = LoginDto(uuid = "test-uuid"),
        registered = null,
        id = null,
        nat = "US"
    )

    private val testUserResponse = UserResponse(
        results = listOf(testUserDto),
        info = InfoDto("test-seed", 1, 1, "1.4")
    )

    private val testUser = User(
        id = "test-uuid",
        gender = "male",
        name = Name("Mr", "John", "Doe"),
        email = "john.doe@example.com",
        phone = "+1234567890",
        cell = "+0987654321",
        picture = Picture("large.jpg", "medium.jpg", "thumbnail.jpg"),
        location = Location(
            street = Street(123, "Main St"),
            city = "New York",
            state = "NY",
            country = "USA",
            postcode = "10001",
            coordinates = Coordinates("40.7128", "-74.0060"),
            timezone = Timezone("-5:00", "Eastern Time")
        ),
        dob = DateOfBirth("1990-01-01T00:00:00.000Z", 33),
        nationality = "US",
        isBookmarked = false
    )

    @Before
    fun setup() {
        api = mockk()
        bookmarkDao = mockk()
        repository = UserRepositoryImpl(api, bookmarkDao)
    }

    @Test
    fun `getUsers should return success when api call succeeds`() = runTest {
        // Given
        coEvery { api.getUsers(1, 25) } returns testUserResponse
        coEvery { bookmarkDao.getBookmarkedUserById(any()) } returns null

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertEquals(1, users?.size)
        assertEquals("test-uuid", users?.first()?.id)
        assertFalse(users?.first()?.isBookmarked ?: true)
    }

    @Test
    fun `getUsers should return error when api call fails`() = runTest {
        // Given
        coEvery { api.getUsers(1, 25) } throws UnknownHostException()

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is DataError.Network)
        assertEquals(NetworkError.NO_INTERNET, (error as DataError.Network).error)
    }

    @Test
    fun `getUsers should mark bookmarked users correctly`() = runTest {
        // Given
        val bookmarkedEntity = BookmarkedUserEntity(
            id = "test-uuid",
            gender = "male",
            title = "Mr",
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

        coEvery { api.getUsers(1, 25) } returns testUserResponse
        coEvery { bookmarkDao.getBookmarkedUserById("test-uuid") } returns bookmarkedEntity

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertTrue(users?.first()?.isBookmarked ?: false)
    }

    @Test
    fun `getUserById should return user if bookmarked`() = runTest {
        // Given
        val bookmarkedEntity = BookmarkedUserEntity(
            id = "test-uuid",
            gender = "male",
            title = "Mr",
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

        coEvery { bookmarkDao.getBookmarkedUserById("test-uuid") } returns bookmarkedEntity

        // When
        val result = repository.getUserById("test-uuid")

        // Then
        assertTrue(result is Result.Success)
        val user = result.getOrNull()
        assertEquals("test-uuid", user?.id)
        assertTrue(user?.isBookmarked ?: false)
    }

    @Test
    fun `getUserById should return error if user not found`() = runTest {
        // Given
        coEvery { bookmarkDao.getBookmarkedUserById("non-existent") } returns null

        // When
        val result = repository.getUserById("non-existent")

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is UserError.UserNotFound)
        assertEquals("non-existent", (error as UserError.UserNotFound).userId)
    }

    @Test
    fun `bookmarkUser should insert user and return success`() = runTest {
        // Given
        coEvery { bookmarkDao.insertBookmarkedUser(any()) } just Runs

        // When
        val result = repository.bookmarkUser(testUser)

        // Then
        assertTrue(result is Result.Success)
        coVerify { bookmarkDao.insertBookmarkedUser(any()) }
    }

    @Test
    fun `removeBookmark should delete user and return success`() = runTest {
        // Given
        coEvery { bookmarkDao.deleteBookmarkedUserById("test-uuid") } just Runs

        // When
        val result = repository.removeBookmark("test-uuid")

        // Then
        assertTrue(result is Result.Success)
        coVerify { bookmarkDao.deleteBookmarkedUserById("test-uuid") }
    }

    @Test
    fun `toggleBookmark should add bookmark if not bookmarked`() = runTest {
        // Given
        val user = testUser.copy(isBookmarked = false)
        coEvery { bookmarkDao.getBookmarkedUserById("test-uuid") } returns null
        coEvery { bookmarkDao.insertBookmarkedUser(any()) } just Runs

        // When
        val result = repository.toggleBookmark(user)

        // Then
        assertTrue(result is Result.Success)
        coVerify { bookmarkDao.insertBookmarkedUser(any()) }
    }

    @Test
    fun `isUserBookmarked should return flow of bookmark status`() = runTest {
        // Given
        every { bookmarkDao.isUserBookmarked("test-uuid") } returns flowOf(true)

        // When
        val result = repository.isUserBookmarked("test-uuid").first()

        // Then
        assertTrue(result)
        verify { bookmarkDao.isUserBookmarked("test-uuid") }
    }
}