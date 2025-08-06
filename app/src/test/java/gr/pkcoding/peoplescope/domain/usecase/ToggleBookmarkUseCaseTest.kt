package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ToggleBookmarkUseCaseTest {

    private lateinit var repository: UserRepository
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase

    private val testUser = User(
        id = "test-id",
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
    )

    @Before
    fun setup() {
        repository = mockk()
        toggleBookmarkUseCase = ToggleBookmarkUseCase(repository)
    }

    @Test
    fun `invoke should call repository toggleBookmark`() = runTest {
        // Given
        coEvery { repository.toggleBookmark(testUser) } returns Result.Success(Unit)

        // When
        val result = toggleBookmarkUseCase(testUser)

        // Then
        coVerify { repository.toggleBookmark(testUser) }
        assertTrue(result.isSuccess())
    }

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        // Given
        coEvery { repository.toggleBookmark(testUser) } returns Result.Success(Unit)

        // When
        val result = toggleBookmarkUseCase(testUser)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `invoke should return error when repository fails`() = runTest {
        // Given
        val error = DataError.Local(LocalError.DATABASE_ERROR)
        coEvery { repository.toggleBookmark(testUser) } returns Result.Error(error)

        // When
        val result = toggleBookmarkUseCase(testUser)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }

    @Test
    fun `invoke should handle user with bookmark status`() = runTest {
        // Given
        val bookmarkedUser = testUser.copy(isBookmarked = true)
        coEvery { repository.toggleBookmark(bookmarkedUser) } returns Result.Success(Unit)

        // When
        val result = toggleBookmarkUseCase(bookmarkedUser)

        // Then
        coVerify { repository.toggleBookmark(bookmarkedUser) }
        assertTrue(result.isSuccess())
    }
}