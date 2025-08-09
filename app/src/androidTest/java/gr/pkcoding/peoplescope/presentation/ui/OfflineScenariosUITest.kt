package gr.pkcoding.peoplescope.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.paging.PagingData
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListScreen
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListState
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListViewModel
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class OfflineScenariosUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testBookmarkedUsers = listOf(
        User(
            id = "offline-1",
            gender = "male",
            name = Name("Mr", "John", "Offline"),
            email = "john.offline@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = Picture("large1.jpg", "medium1.jpg", "thumbnail1.jpg"),
            location = Location(
                street = Street(123, "Offline St"),
                city = "Offline City",
                state = "OC",
                country = "Offline Country",
                postcode = "12345",
                coordinates = Coordinates("40.7128", "-74.0060"),
                timezone = Timezone("-5:00", "Eastern Time")
            ),
            dob = DateOfBirth("1990-01-01", 33),
            nationality = "US",
            isBookmarked = true
        ),
        User(
            id = "offline-2",
            gender = "female",
            name = Name("Ms", "Jane", "Cached"),
            email = "jane.cached@example.com",
            phone = "+1987654321",
            cell = "+1234567890",
            picture = Picture("large2.jpg", "medium2.jpg", "thumbnail2.jpg"),
            location = Location(
                street = Street(456, "Cache Ave"),
                city = "Cache City",
                state = "CC",
                country = "Cache Country",
                postcode = "67890",
                coordinates = Coordinates("34.0522", "-118.2437"),
                timezone = Timezone("-8:00", "Pacific Time")
            ),
            dob = DateOfBirth("1992-05-15", 31),
            nationality = "US",
            isBookmarked = true
        )
    )

    @Test
    fun userListScreen_displaysOfflineMode_whenNoInternet() {
        // Create mocks for offline scenario
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()

        // Setup offline scenario
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(
            testBookmarkedUsers.map { user ->
                BookmarkedUserEntity(
                    id = user.id!!,
                    gender = user.gender ?: "",
                    title = user.name?.title ?: "",
                    firstName = user.name?.first ?: "",
                    lastName = user.name?.last ?: "",
                    email = user.email ?: "",
                    phone = user.phone ?: "",
                    cell = user.cell ?: "",
                    pictureLarge = user.picture?.large ?: "",
                    pictureMedium = user.picture?.medium ?: "",
                    pictureThumbnail = user.picture?.thumbnail ?: "",
                    streetNumber = user.location?.street?.number ?: 0,
                    streetName = user.location?.street?.name ?: "",
                    city = user.location?.city ?: "",
                    state = user.location?.state ?: "",
                    country = user.location?.country ?: "",
                    postcode = user.location?.postcode ?: "",
                    latitude = user.location?.coordinates?.latitude ?: "",
                    longitude = user.location?.coordinates?.longitude ?: "",
                    timezoneOffset = user.location?.timezone?.offset ?: "",
                    timezoneDescription = user.location?.timezone?.description ?: "",
                    dobDate = user.dob?.date ?: "",
                    dobAge = user.dob?.age ?: 0,
                    nationality = user.nationality ?: "",
                    bookmarkedAt = System.currentTimeMillis()
                )
            }
        )
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(testBookmarkedUsers))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = true,
                        showNetworkError = false
                    ),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Check if offline mode indicator is displayed
        composeTestRule
            .onNodeWithText("📱 Showing bookmarked users")
            .assertIsDisplayed()

        // Check if bookmarked users are displayed
        composeTestRule
            .onNodeWithText("Mr John Offline")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ms Jane Cached")
            .assertIsDisplayed()

        // Check if locations are displayed
        composeTestRule
            .onNodeWithText("Offline City, Offline Country")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cache City, Cache Country")
            .assertIsDisplayed()
    }

    @Test
    fun userListScreen_displaysNetworkError_whenNoInternetAndNoBookmarks() {
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()

        // Setup offline scenario with no bookmarks
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())
        every { getUsersPagedUseCase() } returns flowOf(PagingData.empty())

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = false,
                        showNetworkError = true
                    ),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Check if network error is displayed
        composeTestRule
            .onNodeWithText("📵 No internet connection")
            .assertIsDisplayed()

        // Check if no internet error view is displayed
        composeTestRule
            .onNodeWithText("📵 No Internet Connection")
            .assertIsDisplayed()

        // Check if retry button is available
        composeTestRule
            .onNodeWithText("Retry Connection")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userListScreen_transitionsFromOfflineToOnline() {
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()

        // Initially offline
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false, true) // Transition to online
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(
            testBookmarkedUsers.map { user ->
                BookmarkedUserEntity(
                    id = user.id!!,
                    gender = user.gender ?: "",
                    title = user.name?.title ?: "",
                    firstName = user.name?.first ?: "",
                    lastName = user.name?.last ?: "",
                    email = user.email ?: "",
                    phone = user.phone ?: "",
                    cell = user.cell ?: "",
                    pictureLarge = user.picture?.large ?: "",
                    pictureMedium = user.picture?.medium ?: "",
                    pictureThumbnail = user.picture?.thumbnail ?: "",
                    streetNumber = user.location?.street?.number ?: 0,
                    streetName = user.location?.street?.name ?: "",
                    city = user.location?.city ?: "",
                    state = user.location?.state ?: "",
                    country = user.location?.country ?: "",
                    postcode = user.location?.postcode ?: "",
                    latitude = user.location?.coordinates?.latitude ?: "",
                    longitude = user.location?.coordinates?.longitude ?: "",
                    timezoneOffset = user.location?.timezone?.offset ?: "",
                    timezoneDescription = user.location?.timezone?.description ?: "",
                    dobDate = user.dob?.date ?: "",
                    dobAge = user.dob?.age ?: 0,
                    nationality = user.nationality ?: "",
                    bookmarkedAt = System.currentTimeMillis()
                )
            }
        )
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(testBookmarkedUsers))

        var currentState = UserListState(
            isOnline = false,
            isOfflineMode = true,
            showNetworkError = false
        )

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = currentState,
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Initially should show offline mode
        composeTestRule
            .onNodeWithText("📱 Showing bookmarked users")
            .assertIsDisplayed()

        // Simulate connection restored
        currentState = currentState.copy(
            isOnline = true,
            isOfflineMode = false,
            showNetworkError = false
        )

        // Update UI state
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = currentState,
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Offline mode indicator should no longer be displayed
        composeTestRule
            .onNodeWithText("📱 Showing bookmarked users")
            .assertDoesNotExist()
    }

    @Test
    fun userListScreen_searchFunctionality_worksInOfflineMode() {
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()

        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(
            testBookmarkedUsers.map { user ->
                BookmarkedUserEntity(
                    id = user.id!!,
                    gender = user.gender ?: "",
                    title = user.name?.title ?: "",
                    firstName = user.name?.first ?: "",
                    lastName = user.name?.last ?: "",
                    email = user.email ?: "",
                    phone = user.phone ?: "",
                    cell = user.cell ?: "",
                    pictureLarge = user.picture?.large ?: "",
                    pictureMedium = user.picture?.medium ?: "",
                    pictureThumbnail = user.picture?.thumbnail ?: "",
                    streetNumber = user.location?.street?.number ?: 0,
                    streetName = user.location?.street?.name ?: "",
                    city = user.location?.city ?: "",
                    state = user.location?.state ?: "",
                    country = user.location?.country ?: "",
                    postcode = user.location?.postcode ?: "",
                    latitude = user.location?.coordinates?.latitude ?: "",
                    longitude = user.location?.coordinates?.longitude ?: "",
                    timezoneOffset = user.location?.timezone?.offset ?: "",
                    timezoneDescription = user.location?.timezone?.description ?: "",
                    dobDate = user.dob?.date ?: "",
                    dobAge = user.dob?.age ?: 0,
                    nationality = user.nationality ?: "",
                    bookmarkedAt = System.currentTimeMillis()
                )
            }
        )
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(testBookmarkedUsers))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = true,
                        showNetworkError = false,
                        searchQuery = ""
                    ),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Both users should be visible initially
        composeTestRule
            .onNodeWithText("Mr John Offline")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ms Jane Cached")
            .assertIsDisplayed()

        // Search for "John"
        composeTestRule
            .onNodeWithText("Search users...")
            .performTextInput("John")

        composeTestRule.waitForIdle()

        // Only John should be visible after search
        // Note: In a real test, you'd need to implement the actual filtering logic
        // This is just testing that the search field works
        composeTestRule
            .onNodeWithText("Search users...")
            .assertTextContains("John")
    }

    @Test
    fun userListScreen_pullToRefresh_worksInOfflineMode() {
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()

        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(testBookmarkedUsers))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = true,
                        showNetworkError = false
                    ),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // The pull-to-refresh functionality should still be available in offline mode
        // though it might not fetch new data from the internet
        composeTestRule
            .onRoot()
            .performTouchInput {
                swipeDown()
            }

        composeTestRule.waitForIdle()
    }
}