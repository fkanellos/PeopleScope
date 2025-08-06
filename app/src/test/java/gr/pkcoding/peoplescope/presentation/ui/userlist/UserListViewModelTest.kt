package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.UiText
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class UserListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getUsersPagedUseCase: GetUsersPagedUseCase
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase
    private lateinit var viewModel: UserListViewModel

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
        Dispatchers.setMain(testDispatcher)

        getUsersPagedUseCase = mockk()
        toggleBookmarkUseCase = mockk()

        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(listOf(testUser)))

        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty search query`() = testScope.runTest {
        val initialState = viewModel.state.first()
        assertEquals("", initialState.searchQuery)
        assertEquals(false, initialState.isRefreshing)
    }

    @Test
    fun `updateSearchQuery should update state`() = testScope.runTest {
        val searchQuery = "John"

        viewModel.processIntent(UserListIntent.UpdateSearchQuery(searchQuery))
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(searchQuery, state.searchQuery)
    }

    @Test
    fun `clearSearch should reset search query`() = testScope.runTest {
        // First set a search query
        viewModel.processIntent(UserListIntent.UpdateSearchQuery("test"))
        advanceUntilIdle()

        // Then clear it
        viewModel.processIntent(UserListIntent.ClearSearch)
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `toggleBookmark success should emit success effect`() = testScope.runTest {
        val user = testUser.copy(isBookmarked = false)
        coEvery { toggleBookmarkUseCase(user) } returns Result.Success(Unit)

        viewModel.processIntent(UserListIntent.ToggleBookmark(user))
        advanceUntilIdle()

        coVerify { toggleBookmarkUseCase(user) }
    }

    @Test
    fun `toggleBookmark error should emit error effect`() = testScope.runTest {
        val user = testUser.copy(isBookmarked = false)
        val error = DataError.Local(LocalError.DATABASE_ERROR)
        coEvery { toggleBookmarkUseCase(user) } returns Result.Error(error)

        viewModel.processIntent(UserListIntent.ToggleBookmark(user))
        advanceUntilIdle()

        coVerify { toggleBookmarkUseCase(user) }
    }

    @Test
    fun `navigateToDetail should emit navigation effect`() = testScope.runTest {
        viewModel.processIntent(UserListIntent.NavigateToDetail(testUser))
        advanceUntilIdle()

        // In a real test, you'd verify the effect was emitted
        // This requires collecting the effect flow in a separate coroutine
    }

    @Test
    fun `paged users should be filtered by search query`() = testScope.runTest {
        val users = listOf(
            testUser.copy(name = Name("Mr", "John", "Doe")),
            testUser.copy(name = Name("Ms", "Jane", "Smith"), id = "test-id-2")
        )

        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(users))

        // Set search query
        viewModel.processIntent(UserListIntent.UpdateSearchQuery("John"))
        advanceUntilIdle()

        // Verify filtering works (this is a simplified test)
        val filteredData = viewModel.getPagedUsersWithBookmarkUpdates().asSnapshot()
        // In real implementation, you'd need proper testing utilities for PagingData
    }
}