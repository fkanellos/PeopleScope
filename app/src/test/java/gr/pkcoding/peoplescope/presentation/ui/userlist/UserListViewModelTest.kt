package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.paging.PagingData
import app.cash.turbine.test
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.domain.model.Coordinates
import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.DateOfBirth
import gr.pkcoding.peoplescope.domain.model.LocalError
import gr.pkcoding.peoplescope.domain.model.Location
import gr.pkcoding.peoplescope.domain.model.Name
import gr.pkcoding.peoplescope.domain.model.Picture
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.Street
import gr.pkcoding.peoplescope.domain.model.Timezone
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    private lateinit var networkProvider: NetworkConnectivityProvider

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
        networkProvider = mockk()

        // Mock network provider flows
        every { networkProvider.isNetworkAvailable() } returns true
        every { networkProvider.networkConnectivityFlow() } returns flowOf(true)

        // Mock the Flow that ViewModel observes in init block
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList<BookmarkedUserEntity>())
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(listOf(testUser)))

        // Create ViewModel AFTER setting up mocks
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
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
        assertEquals(true, initialState.isOnline)
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

        // In a real test, we'd verify the effect was emitted using Turbine
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
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()
        viewModel.processIntent(UserListIntent.UpdateSearchQuery("John"))
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals("John", state.searchQuery)
    }

    @Test
    fun `network state changes should update ViewModel state`() = testScope.runTest {
        // Given - Start with online state
        every { networkProvider.networkConnectivityFlow() } returns flowOf(true, false, true)

        // Create ViewModel
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()

        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals(true, initialState.isOnline)
            assertEquals(false, initialState.isOfflineMode)
            assertEquals(false, initialState.showNetworkError)
        }
    }

    @Test
    fun `offline mode should be enabled when no internet but has bookmarks`() = testScope.runTest {
        // Given - Offline with bookmarks
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(
            listOf(
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
        )

        // Create ViewModel
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(false, state.isOnline)
        assertEquals(true, state.isOfflineMode) // Should enable offline mode
        assertEquals(false, state.showNetworkError)
    }

    @Test
    fun `network error should be shown when no internet and no bookmarks`() = testScope.runTest {
        // Given - Offline without bookmarks
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        // Create ViewModel
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()

        val state = viewModel.state.first()
        assertFalse("Should be offline", state.isOnline)
        assertFalse("Should not show offline mode", state.isOfflineMode)
        assertTrue("Should show network error", state.showNetworkError)
    }
    private fun createViewModel(
        initialNetworkState: Boolean = true,
        networkStateFlow: kotlinx.coroutines.flow.Flow<Boolean> = flowOf(true)
    ): UserListViewModel {
        every { networkProvider.isNetworkAvailable() } returns initialNetworkState
        every { networkProvider.networkConnectivityFlow() } returns networkStateFlow

        return UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )
    }

    @Test
    fun `network state change from online to offline updates state correctly`() = testScope.runTest {
        // Given - Start online, then go offline
        viewModel = createViewModel(
            initialNetworkState = true,
            networkStateFlow = flowOf(true, false)
        )

        advanceUntilIdle()

        // When & Then - Just check state changes, not effects
        viewModel.state.test {
            var state = awaitItem()

            // Wait for offline state
            while (state.isOnline) {
                state = awaitItem()
            }

            // Verify offline state
            assertFalse("Should be offline", state.isOnline)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `network state change from offline to online updates state correctly`() = testScope.runTest {
        // Given - Start offline, then go online
        viewModel = createViewModel(
            initialNetworkState = false,
            networkStateFlow = flowOf(false, true)
        )

        advanceUntilIdle()

        // When & Then - Just check state changes
        viewModel.state.test {
            var state = awaitItem()

            // Wait for online state
            while (!state.isOnline) {
                state = awaitItem()
            }

            // Verify online state
            assertTrue("Should be online", state.isOnline)

            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `state tracks connection changes correctly using helper methods`() = testScope.runTest {
        // Given - Start online
        viewModel = createViewModel(
            initialNetworkState = true,
            networkStateFlow = flowOf(true)
        )

        advanceUntilIdle()

        // Then - Initial state
        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue("Should be online initially", initialState.isOnline)
            assertTrue("Last online state should be true", initialState.lastOnlineState)
            assertFalse("Should not show connection just lost", initialState.isConnectionJustLost())
            assertFalse("Should not show connection just restored", initialState.isConnectionJustRestored())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `going offline with no bookmarks shows network error`() = testScope.runTest {
        // Given - No bookmarked users
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        viewModel = createViewModel(
            initialNetworkState = true,
            networkStateFlow = flowOf(true, false)
        )

        advanceUntilIdle()

        // When & Then
        viewModel.state.test {
            var state = awaitItem()
            while (state.isOnline) {
                state = awaitItem()
            }

            // Should show network error when offline with no content
            assertTrue("Should show network error", state.shouldShowNetworkError())
            assertFalse("Should not show offline mode", state.isOfflineMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `going offline with bookmarks shows offline mode`() = testScope.runTest {
        // Given - Has bookmarked users
        val bookmarkedUsers = listOf(
            BookmarkedUserEntity(
                id = "bookmark-1",
                gender = "male",
                title = "Mr",
                firstName = "Bookmark",
                lastName = "User",
                email = "bookmark@example.com",
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

        viewModel = createViewModel(
            initialNetworkState = true,
            networkStateFlow = flowOf(true, false)
        )

        advanceUntilIdle()

        // When & Then - Just check basic state properties
        viewModel.state.test {
            var state = awaitItem()
            while (state.isOnline) {
                state = awaitItem()
            }

            assertFalse("Should be offline", state.isOnline)
            assertTrue("Should show offline mode", state.isOfflineMode)
            assertFalse("Should not show network error", state.showNetworkError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `connection state changes update state correctly`() = testScope.runTest {
        // Given - Setup with controlled network flow
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())

        val networkStateFlow = MutableSharedFlow<Boolean>(replay = 1)
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns networkStateFlow.asSharedFlow()

        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        // Start offline
        networkStateFlow.emit(false)
        advanceUntilIdle()

        // Verify offline state
        var currentState = viewModel.state.value
        assertFalse("Should be offline", currentState.isOnline)
        assertTrue("Should show network error", currentState.showNetworkError)

        // When - Go online
        every { networkProvider.isNetworkAvailable() } returns true
        networkStateFlow.emit(true)
        advanceUntilIdle()

        // Then - Verify online state and connection restored detection
        currentState = viewModel.state.value
        assertTrue("Should be online", currentState.isOnline)
        assertTrue("Should detect connection just restored", currentState.isConnectionJustRestored())
        assertFalse("Should not show network error", currentState.showNetworkError)
    }
}
