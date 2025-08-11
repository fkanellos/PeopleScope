package gr.pkcoding.peoplescope.data.repository

import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.data.remote.dto.CoordinatesDto
import gr.pkcoding.peoplescope.data.remote.dto.DobDto
import gr.pkcoding.peoplescope.data.remote.dto.IdDto
import gr.pkcoding.peoplescope.data.remote.dto.InfoDto
import gr.pkcoding.peoplescope.data.remote.dto.LocationDto
import gr.pkcoding.peoplescope.data.remote.dto.LoginDto
import gr.pkcoding.peoplescope.data.remote.dto.NameDto
import gr.pkcoding.peoplescope.data.remote.dto.PictureDto
import gr.pkcoding.peoplescope.data.remote.dto.RegisteredDto
import gr.pkcoding.peoplescope.data.remote.dto.StreetDto
import gr.pkcoding.peoplescope.data.remote.dto.TimezoneDto
import gr.pkcoding.peoplescope.data.remote.dto.UserDto
import gr.pkcoding.peoplescope.data.remote.dto.UserResponse
import gr.pkcoding.peoplescope.domain.model.Coordinates
import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.DateOfBirth
import gr.pkcoding.peoplescope.domain.model.LocalError
import gr.pkcoding.peoplescope.domain.model.Location
import gr.pkcoding.peoplescope.domain.model.Name
import gr.pkcoding.peoplescope.domain.model.NetworkError
import gr.pkcoding.peoplescope.domain.model.Picture
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.Street
import gr.pkcoding.peoplescope.domain.model.Timezone
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.model.UserError
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    private lateinit var networkProvider: NetworkConnectivityProvider

    private val testUserDto = UserDto(
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
        networkProvider = mockk()

        // Setup default network state (online)
        every { networkProvider.isNetworkAvailable() } returns true
        every { networkProvider.networkConnectivityFlow() } returns flowOf(true)

        repository = UserRepositoryImpl(api, bookmarkDao, networkProvider)
    }

    @Test
    fun `getUsers should return success when api call succeeds`() = runTest {
        // Given
        coEvery { api.getUsers(1, 25) } returns testUserResponse
        coEvery { bookmarkDao.getBookmarkedUserIds() } returns emptyList()

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
    fun `getUsers should return error when api call fails on page 2`() = runTest {
        // Given - API fails on page 2 (no fallback)
        coEvery { api.getUsers(2, 25) } throws UnknownHostException()
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        // When - Request page 2
        val result = repository.getUsers(2, 25)

        // Then - Should return error (no fallback for page > 1)
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is DataError.Network)
        assertEquals(NetworkError.NO_INTERNET, (error as DataError.Network).error)
    }

    @Test
    fun `getUsers should fallback to bookmarks when api call fails on page 1`() = runTest {
        // Given - API fails on page 1, but we have bookmarked users
        coEvery { api.getUsers(1, 25) } throws UnknownHostException()

        val bookmarkedUsers = listOf(
            BookmarkedUserEntity(
                id = "fallback-user",
                gender = "male",
                title = "Mr",
                firstName = "Fallback",
                lastName = "User",
                email = "fallback@example.com",
                phone = "+1234567890",
                cell = "+0987654321",
                pictureLarge = "large.jpg",
                pictureMedium = "medium.jpg",
                pictureThumbnail = "thumb.jpg",
                streetNumber = 123,
                streetName = "Street",
                city = "City",
                state = "State",
                country = "Country",
                postcode = "12345",
                latitude = "0",
                longitude = "0",
                timezoneOffset = "+0",
                timezoneDescription = "UTC",
                dobDate = "1990-01-01",
                dobAge = 33,
                nationality = "US",
                bookmarkedAt = System.currentTimeMillis()
            )
        )

        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(bookmarkedUsers)

        // When - Request page 1
        val result = repository.getUsers(1, 25)

        // Then - Should return bookmarked users as fallback (Success)
        assertTrue("Should fallback to bookmarks successfully", result is Result.Success)
        val users = result.getOrNull()
        assertNotNull("Fallback users should not be null", users)
        assertEquals("Should return fallback users", 1, users!!.size)
        assertEquals("Should return correct fallback user", "Fallback", users.first().name?.first)
        assertTrue("Fallback user should be bookmarked", users.first().isBookmarked)
    }

    @Test
    fun `getUsers should mark bookmarked users correctly`() = runTest {
        // Given
        coEvery { api.getUsers(1, 25) } returns testUserResponse
        coEvery { bookmarkDao.getBookmarkedUserIds() } returns listOf("test-uuid")

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertNotNull(users)
        assertTrue("User should be bookmarked", users!!.first().isBookmarked)
    }

    @Test
    fun `getUsers should return empty list when no bookmarked users`() = runTest {
        // Given
        coEvery { api.getUsers(1, 25) } returns testUserResponse
        coEvery { bookmarkDao.getBookmarkedUserIds() } returns emptyList()

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertNotNull(users)
        assertFalse("User should not be bookmarked", users!!.first().isBookmarked)
    }

    @Test
    fun `getUsers handles nullable fields correctly`() = runTest {
        // Given - UserDto με null fields
        val userDtoWithNulls = UserDto(
            gender = null,
            name = NameDto("Mr", "John", null),
            location = null,
            email = null,
            login = LoginDto(uuid = "test-uuid", username = null, password = null, salt = null, md5 = null, sha1 = null, sha256 = null),
            dob = null,
            registered = null,
            phone = null,
            cell = null,
            id = null,
            picture = null,
            nat = null
        )

        val responseWithNulls = UserResponse(
            results = listOf(userDtoWithNulls),
            info = InfoDto("test-seed", 1, 1, "1.4")
        )

        coEvery { api.getUsers(1, 25) } returns responseWithNulls
        coEvery { bookmarkDao.getBookmarkedUserIds() } returns emptyList()

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertNotNull(users)
        assertTrue("Invalid users should be filtered out", users!!.isEmpty())
    }

    @Test
    fun `toggleBookmark should handle null user ID gracefully`() = runTest {
        // Given - User με null ID
        val userWithNullId = testUser.copy(id = null)

        // When
        val result = repository.toggleBookmark(userWithNullId)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(LocalError.DATABASE_ERROR, error.error)
    }

    @Test
    fun `getUserById should return error for null userId`() = runTest {
        val emptyUserId = ""

        coEvery { bookmarkDao.getBookmarkedUserById(emptyUserId) } returns null

        // When
        val result = repository.getUserById(emptyUserId)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is UserError.UserNotFound)
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

    @Test
    fun `getUserById should return bookmarked user when available`() = runTest {
        // Given - User exists in bookmarks
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
        assertEquals("John", user?.name?.first)
        assertEquals(true, user?.isBookmarked)
    }

    @Test
    fun `getUserById should return cached user with bookmark status when available`() = runTest {
        // Given - User in cache but also bookmarked
        coEvery { api.getUsers(1, 25) } returns testUserResponse

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
        coEvery { bookmarkDao.getBookmarkedUserIds() } returns listOf("test-uuid") // ✅ FIX: Add this

        // Populate cache first
        repository.getUsers(1, 25)

        // When - getUserById should find cached user and update bookmark status
        val result = repository.getUserById("test-uuid")

        // Then
        assertTrue(result is Result.Success)
        val user = result.getOrNull()
        assertEquals("test-uuid", user?.id)
        assertEquals("John", user?.name?.first)
        assertEquals(true, user?.isBookmarked) // Should be bookmarked now

        // Verify that we checked the bookmark status
        coVerify { bookmarkDao.getBookmarkedUserById("test-uuid") }
    }

    @Test
    fun `getUsers should return offline users when no network available`() = runTest {
        // Given - No network available
        every { networkProvider.isNetworkAvailable() } returns false

        val offlineUsers = listOf(
            BookmarkedUserEntity(
                id = "offline-user",
                gender = "male",
                title = "Mr",
                firstName = "Offline",
                lastName = "User",
                email = "offline@example.com",
                phone = "+1234567890",
                cell = "+0987654321",
                pictureLarge = "large.jpg",
                pictureMedium = "medium.jpg",
                pictureThumbnail = "thumb.jpg",
                streetNumber = 123,
                streetName = "Street",
                city = "City",
                state = "State",
                country = "Country",
                postcode = "12345",
                latitude = "0",
                longitude = "0",
                timezoneOffset = "+0",
                timezoneDescription = "UTC",
                dobDate = "1990-01-01",
                dobAge = 33,
                nationality = "US",
                bookmarkedAt = System.currentTimeMillis()
            )
        )

        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(offlineUsers)

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue("Should succeed in offline mode", result is Result.Success)
        val users = result.getOrNull()
        assertNotNull("Users should not be null", users)
        assertEquals("Should return offline users", 1, users!!.size)
        assertEquals("Should return correct offline user", "Offline", users.first().name?.first)
        assertTrue("Offline user should be bookmarked", users.first().isBookmarked)
    }

    @Test
    fun `getUsers should return empty list when offline and no bookmarks`() = runTest {
        // Given - No network and no bookmarks
        every { networkProvider.isNetworkAvailable() } returns false
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        // When
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue("Should succeed even with no data", result is Result.Success)
        val users = result.getOrNull()
        assertNotNull("Users should not be null", users)
        assertTrue("Should return empty list", users!!.isEmpty())
    }

    @Test
    fun `getUsers should fallback to bookmarks when network call fails on first page`() = runTest {
        // Given - Network available but API fails
        every { networkProvider.isNetworkAvailable() } returns true
        coEvery { api.getUsers(1, 25) } throws UnknownHostException()

        val bookmarkedUsers = listOf(
            BookmarkedUserEntity(
                id = "fallback-user",
                gender = "female",
                title = "Ms",
                firstName = "Fallback",
                lastName = "User",
                email = "fallback@example.com",
                phone = "+1234567890",
                cell = "+0987654321",
                pictureLarge = "large.jpg",
                pictureMedium = "medium.jpg",
                pictureThumbnail = "thumb.jpg",
                streetNumber = 123,
                streetName = "Street",
                city = "City",
                state = "State",
                country = "Country",
                postcode = "12345",
                latitude = "0",
                longitude = "0",
                timezoneOffset = "+0",
                timezoneDescription = "UTC",
                dobDate = "1990-01-01",
                dobAge = 33,
                nationality = "US",
                bookmarkedAt = System.currentTimeMillis()
            )
        )

        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(bookmarkedUsers)

        // When - First page should fallback to bookmarks
        val result = repository.getUsers(1, 25)

        // Then
        assertTrue("Should fallback to bookmarks successfully", result is Result.Success)
        val users = result.getOrNull()
        assertNotNull("Fallback users should not be null", users)
        assertEquals("Should return fallback users", 1, users!!.size)
        assertEquals("Should return correct fallback user", "Fallback", users.first().name?.first)
        assertTrue("Fallback user should be bookmarked", users.first().isBookmarked)
    }

    @Test
    fun `getUsers should return network error for subsequent pages when network fails`() = runTest {
        // Given - Network available but API fails for page 2
        every { networkProvider.isNetworkAvailable() } returns true
        coEvery { api.getUsers(2, 25) } throws UnknownHostException()
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        // When - Second page should return network error
        val result = repository.getUsers(2, 25)

        // Then
        assertTrue("Should return network error for subsequent pages", result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue("Error should be network related", error is DataError.Network)
        assertEquals("Should be NO_INTERNET error", NetworkError.NO_INTERNET, (error as DataError.Network).error)
    }
}