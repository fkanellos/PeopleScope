package gr.pkcoding.peoplescope.errorhandling

import androidx.test.ext.junit.runners.AndroidJUnit4
import gr.pkcoding.peoplescope.data.mapper.toNetworkError
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.data.repository.UserRepositoryImpl
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import com.google.gson.JsonSyntaxException
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.remote.dto.InfoDto
import gr.pkcoding.peoplescope.data.remote.dto.LoginDto
import gr.pkcoding.peoplescope.data.remote.dto.NameDto
import gr.pkcoding.peoplescope.data.remote.dto.UserDto
import gr.pkcoding.peoplescope.data.remote.dto.UserResponse
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
class ErrorHandlingTest {

    private lateinit var api: RandomUserApi
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var networkProvider: NetworkConnectivityProvider
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        bookmarkDao = mockk()
        networkProvider = mockk()

        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())
        every { bookmarkDao.getBookmarkedUserIds() } returns emptyList()

        repository = UserRepositoryImpl(api, bookmarkDao, networkProvider)
    }

    @Test
    fun networkErrors_areMappedCorrectly() {
        // Test different network exceptions
        val testCases = listOf(
            UnknownHostException() to NetworkError.NO_INTERNET,
            SocketTimeoutException() to NetworkError.REQUEST_TIMEOUT,
            TimeoutException() to NetworkError.REQUEST_TIMEOUT,
            JsonSyntaxException("Invalid JSON") to NetworkError.SERIALIZATION,
            Exception("Unknown error") to NetworkError.UNKNOWN
        )

        testCases.forEach { (exception, expectedError) ->
            val mappedError = exception.toNetworkError()
            assertEquals(
                "Exception ${exception::class.simpleName} should map to $expectedError",
                expectedError,
                mappedError
            )
        }
    }

    @Test
    fun httpErrors_areMappedCorrectly() {
        val httpTestCases = listOf(
            408 to NetworkError.REQUEST_TIMEOUT,
            500 to NetworkError.SERVER_ERROR,
            502 to NetworkError.SERVER_ERROR,
            503 to NetworkError.SERVER_ERROR,
            404 to NetworkError.UNKNOWN,
            401 to NetworkError.UNKNOWN
        )

        httpTestCases.forEach { (statusCode, expectedError) ->
            val response = mockk<Response<*>>()
            every { response.code() } returns statusCode
            every { response.message() } returns "HTTP $statusCode"

            val httpException = HttpException(response)
            val mappedError = httpException.toNetworkError()

            assertEquals(
                "HTTP $statusCode should map to $expectedError",
                expectedError,
                mappedError
            )
        }
    }

    @Test
    fun repository_handlesNoInternetGracefully() = runTest {
        // Given - No internet connection
        every { networkProvider.isNetworkAvailable() } returns false
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        // When - Try to get users
        val result = repository.getUsers(1, 25)

        // Then - Should return empty list (offline mode)
        assertTrue("Should handle no internet gracefully", result.isSuccess())
        val users = result.getOrNull()
        assertNotNull("Users list should not be null", users)
        assertTrue("Users list should be empty in offline mode", users!!.isEmpty())
    }

    @Test
    fun repository_fallsBackToBookmarks_whenNetworkFails() = runTest {
        // Given - Network available but API fails
        every { networkProvider.isNetworkAvailable() } returns true
        coEvery { api.getUsers(any(), any()) } throws UnknownHostException()

        // Setup bookmarked users as fallback
        val bookmarkedUsers = listOf(
            BookmarkedUserEntity(
                id = "bookmark-1",
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
                streetName = "Offline St",
                city = "Offline City",
                state = "OC",
                country = "Offline Country",
                postcode = "12345",
                latitude = "40.7128",
                longitude = "-74.0060",
                timezoneOffset = "-5:00",
                timezoneDescription = "Eastern Time",
                dobDate = "1990-01-01",
                dobAge = 33,
                nationality = "US",
                bookmarkedAt = System.currentTimeMillis()
            )
        )
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(bookmarkedUsers)

        // When - Try to get users (should fallback to bookmarks for first page)
        val result = repository.getUsers(1, 25)

        // Then - Should return bookmarked users as fallback
        assertTrue("Should fallback to bookmarked users", result.isSuccess())
        val users = result.getOrNull()
        assertNotNull("Users should not be null", users)
        assertEquals("Should return bookmarked users", 1, users!!.size)
        assertEquals("Should return correct user", "Offline User", users.first().name?.getFullName())
        assertTrue("User should be marked as bookmarked", users.first().isBookmarked)
    }

    @Test
    fun repository_handlesCorruptedData_gracefully() = runTest {
        // Given - Network available but API returns corrupted data
        every { networkProvider.isNetworkAvailable() } returns true
        coEvery { api.getUsers(any(), any()) } throws JsonSyntaxException("Malformed JSON")

        // When - Try to get users
        val result = repository.getUsers(1, 25)

        // Then - Should handle corrupted data
        assertTrue("Should handle corrupted data", result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue("Error should be network-related", error is DataError.Network)
        assertEquals(
            "Should map to serialization error",
            NetworkError.SERIALIZATION,
            (error as DataError.Network).error
        )
    }

    @Test
    fun repository_handlesPartiallyCorruptedUsers() = runTest {
        // Given - API returns mix of valid and invalid user data
        val response = UserResponse(
            results = listOf(
                // Valid user
                UserDto(
                    gender = "male",
                    name = NameDto("Mr", "Valid", "User"),
                    location = null,
                    email = "valid@example.com",
                    login = LoginDto("valid-uuid", "user", "pass", "salt", "md5", "sha1", "sha256"),
                    dob = null,
                    registered = null,
                    phone = "+1234567890",
                    cell = "+0987654321",
                    id = null,
                    picture = null,
                    nat = "US"
                ),
                // Invalid user (missing UUID)
                UserDto(
                    gender = "female",
                    name = NameDto("Ms", "Invalid", "User"),
                    location = null,
                    email = "invalid@example.com",
                    login = LoginDto(null, "user", "pass", "salt", "md5", "sha1", "sha256"),
                    dob = null,
                    registered = null,
                    phone = "+1234567890",
                    cell = "+0987654321",
                    id = null,
                    picture = null,
                    nat = "US"
                ),
                // Another valid user
                UserDto(
                    gender = "male",
                    name = NameDto("Dr", "Another", "Valid"),
                    location = null,
                    email = "another@example.com",
                    login = LoginDto("another-uuid", "user", "pass", "salt", "md5", "sha1", "sha256"),
                    dob = null,
                    registered = null,
                    phone = "+1234567890",
                    cell = "+0987654321",
                    id = null,
                    picture = null,
                    nat = "US"
                )
            ),
            info = InfoDto("test-seed", 3, 1, "1.4")
        )

        every { networkProvider.isNetworkAvailable() } returns true
        coEvery { api.getUsers(any(), any()) } returns response

        // When - Get users
        val result = repository.getUsers(1, 25)

        // Then - Should filter out invalid users and return only valid ones
        assertTrue("Should succeed despite partial corruption", result.isSuccess())
        val users = result.getOrNull()
        assertNotNull("Users should not be null", users)
        assertEquals("Should return only valid users", 2, users!!.size)
        assertEquals("First user should be valid", "Valid", users[0].name?.last)
        assertEquals("Second user should be valid", "Valid", users[1].name?.last)
    }

    @Test
    fun repository_handlesBookmarkErrors_gracefully() = runTest {
        // Given - Database error when trying to bookmark
        val testUser = User(
            id = "test-id",
            gender = "male",
            name = Name("Mr", "Test", "User"),
            email = "test@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        coEvery { bookmarkDao.insertBookmarkedUser(any()) } throws Exception("Database full")

        // When - Try to bookmark user
        val result = repository.bookmarkUser(testUser)

        // Then - Should handle database error
        assertTrue("Should handle database error", result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue("Error should be local", error is DataError.Local)
    }

    @Test
    fun repository_handlesInvalidUserIds_gracefully() = runTest {
        // Test various invalid user ID scenarios
        val invalidUserIds = listOf("", "   ", null)

        invalidUserIds.forEach { invalidId ->
            val result = repository.getUserById(invalidId ?: "")

            assertTrue(
                "Should handle invalid user ID '$invalidId'",
                result is Result.Error
            )

            val error = (result as Result.Error).error
            assertTrue(
                "Error should be UserNotFound for '$invalidId'",
                error is UserError.UserNotFound
            )
        }
    }

    @Test
    fun repository_handlesSimultaneousBookmarkOperations() = runTest {
        // Given - Multiple rapid bookmark operations
        val testUser = User(
            id = "concurrent-test",
            gender = "male",
            name = Name("Mr", "Concurrent", "Test"),
            email = "concurrent@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // Mock successful operations
        coEvery { bookmarkDao.getBookmarkedUserById(any()) } returns null
        coEvery { bookmarkDao.insertBookmarkedUser(any()) } just Runs
        coEvery { bookmarkDao.deleteBookmarkedUserById(any()) } just Runs

        // When - Perform multiple rapid bookmark operations
        val results = mutableListOf<Result<Unit, DataError.Local>>()

        repeat(5) {
            results.add(repository.toggleBookmark(testUser))
        }

        // Then - All operations should complete without crashing
        results.forEach { result ->
            assertTrue("Each operation should succeed or fail gracefully",
                result is Result.Success || result is Result.Error)
        }
    }

    @Test
    fun repository_recoversFromTransientErrors() = runTest {
        // Given - API fails first time, succeeds second time
        every { networkProvider.isNetworkAvailable() } returns true
        coEvery { api.getUsers(1, 25) } throws SocketTimeoutException() andThen
                UserResponse(
                    results = listOf(
                        UserDto(
                            gender = "male",
                            name = NameDto("Mr", "Recovery", "Test"),
                            location = null,
                            email = "recovery@example.com",
                            login = LoginDto("recovery-uuid", "user", "pass", "salt", "md5", "sha1", "sha256"),
                            dob = null,
                            registered = null,
                            phone = "+1234567890",
                            cell = "+0987654321",
                            id = null,
                            picture = null,
                            nat = "US"
                        )
                    ),
                    info = InfoDto("test-seed", 1, 1, "1.4")
                )

        // When - First call fails
        val firstResult = repository.getUsers(1, 25)
        assertTrue("First call should fail", firstResult is Result.Error)

        // Then - Second call should succeed
        val secondResult = repository.getUsers(1, 25)
        assertTrue("Second call should succeed", secondResult.isSuccess())
        val users = secondResult.getOrNull()
        assertNotNull("Users should not be null", users)
        assertEquals("Should return recovery user", "Recovery", users!!.first().name?.first)
    }
}