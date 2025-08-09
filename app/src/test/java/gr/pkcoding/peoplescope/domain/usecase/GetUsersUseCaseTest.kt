package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import gr.pkcoding.peoplescope.utils.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetUsersUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var getUsersUseCase: GetUsersUseCase

    private val testUsers = listOf(
        User(
            id = "1",
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
            dob = DateOfBirth("1990-01-01", 33),
            nationality = "US",
            isBookmarked = false
        ),
        User(
            id = "2",
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
            dob = DateOfBirth("1992-05-15", 31),
            nationality = "US",
            isBookmarked = true
        )
    )

    @Before
    fun setup() {
        repository = mockk()
        getUsersUseCase = GetUsersUseCase(repository)
    }

    @Test
    fun `invoke should call repository with default parameters`() = runTest {
        // Given
        coEvery { repository.getUsers(Constants.INITIAL_PAGE, Constants.PAGE_SIZE) } returns Result.Success(testUsers)

        // When
        val result = getUsersUseCase()

        // Then
        coVerify { repository.getUsers(Constants.INITIAL_PAGE, Constants.PAGE_SIZE) }
        assertTrue(result is Result.Success)
        assertEquals(testUsers, result.getOrNull())
    }

    @Test
    fun `invoke should call repository with custom parameters`() = runTest {
        // Given
        val customPage = 2
        val customPageSize = 10
        coEvery { repository.getUsers(customPage, customPageSize) } returns Result.Success(testUsers)

        // When
        val result = getUsersUseCase(customPage, customPageSize)

        // Then
        coVerify { repository.getUsers(customPage, customPageSize) }
        assertTrue(result is Result.Success)
        assertEquals(testUsers, result.getOrNull())
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        // Given
        coEvery { repository.getUsers(any(), any()) } returns Result.Success(testUsers)

        // When
        val result = getUsersUseCase()

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertEquals(2, users?.size)
        assertEquals("John", users?.first()?.name?.first)
        assertEquals(true, users?.last()?.isBookmarked)
    }

    @Test
    fun `invoke should return error when repository fails with network error`() = runTest {
        // Given
        val networkError = DataError.Network(NetworkError.NO_INTERNET)
        coEvery { repository.getUsers(any(), any()) } returns Result.Error(networkError)

        // When
        val result = getUsersUseCase()

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is DataError.Network)
        assertEquals(NetworkError.NO_INTERNET, (error as DataError.Network).error)
    }

    @Test
    fun `invoke should return error when repository fails with local error`() = runTest {
        // Given
        val localError = DataError.Local(LocalError.DATABASE_ERROR)
        coEvery { repository.getUsers(any(), any()) } returns Result.Error(localError)

        // When
        val result = getUsersUseCase()

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is DataError.Local)
        assertEquals(LocalError.DATABASE_ERROR, (error as DataError.Local).error)
    }

    @Test
    fun `invoke should return empty list when repository returns empty`() = runTest {
        // Given
        coEvery { repository.getUsers(any(), any()) } returns Result.Success(emptyList())

        // When
        val result = getUsersUseCase()

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertNotNull(users)
        assertTrue(users!!.isEmpty())
    }

    @Test
    fun `invoke should handle mixed bookmark states correctly`() = runTest {
        // Given
        val mixedUsers = listOf(
            testUsers[0].copy(isBookmarked = false),
            testUsers[1].copy(isBookmarked = true)
        )
        coEvery { repository.getUsers(any(), any()) } returns Result.Success(mixedUsers)

        // When
        val result = getUsersUseCase()

        // Then
        assertTrue(result is Result.Success)
        val users = result.getOrNull()
        assertNotNull(users)
        assertEquals(2, users!!.size)
        assertFalse("First user should not be bookmarked", users[0].isBookmarked)
        assertTrue("Second user should be bookmarked", users[1].isBookmarked)
    }
}