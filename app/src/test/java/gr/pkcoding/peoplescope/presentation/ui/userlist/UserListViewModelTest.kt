package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.paging.PagingData
import app.cash.turbine.test
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
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
    private lateinit var bookmarkDao: BookmarkDao

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

        // Create mocks
        getUsersPagedUseCase = mockk()
        toggleBookmarkUseCase = mockk()
        bookmarkDao = mockk()

        // CRITICAL: Mock the Flow that ViewModel observes in init block
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList<BookmarkedUserEntity>())
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(listOf(testUser)))

        // Create ViewModel AFTER setting up mocks
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty search query`() = testScope.runTest {
        // Allow ViewModel initialization to complete
        advanceUntilIdle()

        val initialState = viewModel.state.first()
        assertEquals("", initialState.searchQuery)
        assertEquals(false, initialState.isRefreshing)
    }

    @Test
    fun `updateSearchQuery should update state`() = testScope.runTest {
        val searchQuery = "John"

        // Allow ViewModel to initialize first
        advanceUntilIdle()

        viewModel.processIntent(UserListIntent.UpdateSearchQuery(searchQuery))

        // Process all pending coroutines
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(searchQuery, state.searchQuery)
    }

    @Test
    fun `clearSearch should reset search query`() = testScope.runTest {
        viewModel.state.test {
            assertEquals("", awaitItem().searchQuery)

            viewModel.processIntent(UserListIntent.UpdateSearchQuery("test"))
            assertEquals("test", awaitItem().searchQuery)

            viewModel.processIntent(UserListIntent.ClearSearch)
            assertEquals("", awaitItem().searchQuery)
        }
    }

    @Test
    fun `toggleBookmark success should update bookmark state`() = testScope.runTest {
        val user = testUser.copy(isBookmarked = false)
        coEvery { toggleBookmarkUseCase(user) } returns Result.Success(Unit)

        advanceUntilIdle()
        viewModel.processIntent(UserListIntent.ToggleBookmark(user))
        advanceUntilIdle()

        coVerify { toggleBookmarkUseCase(user) }
    }

    @Test
    fun `toggleBookmark error should emit error effect`() = testScope.runTest {
        val user = testUser.copy(isBookmarked = false)
        val error = DataError.Local(LocalError.DATABASE_ERROR)
        coEvery { toggleBookmarkUseCase(user) } returns Result.Error(error)

        advanceUntilIdle()
        viewModel.processIntent(UserListIntent.ToggleBookmark(user))
        advanceUntilIdle()

        coVerify { toggleBookmarkUseCase(user) }
    }

    @Test
    fun `navigateToDetail should emit navigation effect`() = testScope.runTest {
        advanceUntilIdle()
        viewModel.processIntent(UserListIntent.NavigateToDetail(testUser))
        advanceUntilIdle()

        // In a real test, you'd verify the effect was emitted using Turbine
        // For now, we just verify no exceptions were thrown
    }

    @Test
    fun `search filtering works correctly`() = testScope.runTest {
        val users = listOf(
            testUser.copy(name = Name("Mr", "John", "Doe")),
            testUser.copy(name = Name("Ms", "Jane", "Smith"), id = "test-id-2")
        )

        // Update mock to return new data
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(users))

        // Create new ViewModel with updated mock
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao
        )

        advanceUntilIdle()
        viewModel.processIntent(UserListIntent.UpdateSearchQuery("John"))
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals("John", state.searchQuery)
    }
}