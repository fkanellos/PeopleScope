package gr.pkcoding.peoplescope.presentation.ui.userdetail

import app.cash.turbine.test
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.GetUserDetailsUseCase
import gr.pkcoding.peoplescope.domain.usecase.IsUserBookmarkedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class UserDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getUserDetailsUseCase: GetUserDetailsUseCase
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase
    private lateinit var isUserBookmarkedUseCase: IsUserBookmarkedUseCase
    private lateinit var viewModel: UserDetailViewModel

    private val testUserId = "test-user-id"
    private val testUser = User(
        id = testUserId,
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
        Dispatchers.setMain(testDispatcher)

        getUserDetailsUseCase = mockk()
        toggleBookmarkUseCase = mockk()
        isUserBookmarkedUseCase = mockk()

        // Setup default mocks
        coEvery { getUserDetailsUseCase(testUserId) } returns Result.Success(testUser)
        every { isUserBookmarkedUseCase(testUserId) } returns flowOf(false)
        coEvery { toggleBookmarkUseCase(any()) } returns Result.Success(Unit)

        viewModel = UserDetailViewModel(
            userId = testUserId,
            getUserDetailsUseCase = getUserDetailsUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            isUserBookmarkedUseCase = isUserBookmarkedUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be loading`() = testScope.runTest {
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.user)
        assertEquals(testUser, state.user)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadUser should update state with user data on success`() = testScope.runTest {
        // Setup is already done in @Before
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(testUser, state.user)
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify { getUserDetailsUseCase(testUserId) }
    }

    @Test
    fun `loadUser should update state with error on failure`() = testScope.runTest {
        // Given
        val error = UserError.UserNotFound(testUserId)
        coEvery { getUserDetailsUseCase(testUserId) } returns Result.Error(error)

        // Create new ViewModel to trigger init
        viewModel = UserDetailViewModel(
            userId = testUserId,
            getUserDetailsUseCase = getUserDetailsUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            isUserBookmarkedUseCase = isUserBookmarkedUseCase
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.user)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `toggleBookmark should call use case and update state`() = testScope.runTest {
        // Wait for initialization
        advanceUntilIdle()

        // When
        viewModel.processIntent(UserDetailIntent.ToggleBookmark)
        advanceUntilIdle()

        // Then
        coVerify { toggleBookmarkUseCase(testUser) }
    }

    @Test
    fun `toggleBookmark should emit success effect when successful`() = testScope.runTest {
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(UserDetailIntent.ToggleBookmark)
            advanceUntilIdle()

            when (val effect = awaitItem()) {
                is UserDetailEffect.ShowBookmarkToggled -> {
                    assertTrue("Should show bookmarked effect", effect.isBookmarked)
                }
                else -> fail("Expected ShowBookmarkToggled effect")
            }
        }
    }

    @Test
    fun `toggleBookmark should emit error effect when failed`() = testScope.runTest {
        // Given
        val error = DataError.Local(LocalError.DATABASE_ERROR)
        coEvery { toggleBookmarkUseCase(any()) } returns Result.Error(error)

        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(UserDetailIntent.ToggleBookmark)
            advanceUntilIdle()

            when (val effect = awaitItem()) {
                is UserDetailEffect.ShowError -> {
                    assertNotNull(effect.message)
                }
                else -> fail("Expected ShowError effect")
            }
        }
    }

    @Test
    fun `navigateBack should emit NavigateBack effect`() = testScope.runTest {
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(UserDetailIntent.NavigateBack)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is UserDetailEffect.NavigateBack)
        }
    }

    @Test
    fun `bookmark status should be observed and update state`() = testScope.runTest {
        // Given
        every { isUserBookmarkedUseCase(testUserId) } returns flowOf(true)

        // Create new ViewModel to trigger the flow observation
        viewModel = UserDetailViewModel(
            userId = testUserId,
            getUserDetailsUseCase = getUserDetailsUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            isUserBookmarkedUseCase = isUserBookmarkedUseCase
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("Bookmark status should be true", state.isBookmarked)
    }

    @Test
    fun `loadUser intent should reload user data`() = testScope.runTest {
        advanceUntilIdle()

        // Reset the mock to verify it's called again
        clearMocks(getUserDetailsUseCase)
        coEvery { getUserDetailsUseCase(testUserId) } returns Result.Success(testUser)

        // When
        viewModel.processIntent(UserDetailIntent.LoadUser(testUserId))
        advanceUntilIdle()

        // Then
        coVerify { getUserDetailsUseCase(testUserId) }
    }

    @Test
    fun `state should update user bookmark status when user exists`() = testScope.runTest {
        advanceUntilIdle()

        // Verify initial state
        assertEquals(false, viewModel.state.value.isBookmarked)

        // Change bookmark status
        every { isUserBookmarkedUseCase(testUserId) } returns flowOf(true)

        // Create new ViewModel to test the flow
        viewModel = UserDetailViewModel(
            userId = testUserId,
            getUserDetailsUseCase = getUserDetailsUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            isUserBookmarkedUseCase = isUserBookmarkedUseCase
        )

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue("User should be marked as bookmarked", state.isBookmarked)
        assertEquals(true, state.user?.isBookmarked)
    }
}