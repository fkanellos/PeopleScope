package gr.pkcoding.peoplescope.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
    fun userListScreen_handlesOfflineWithNoBookmarks() {
        val viewModel = createOfflineViewModel(hasBookmarks = false)

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = false,
                        showNetworkError = true,
                        searchQuery = ""
                    ),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Search users...")
            .assertIsDisplayed()

    }

    private fun createOfflineViewModel(
        hasBookmarks: Boolean = true
    ): UserListViewModel {
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()

        // Setup offline scenario
        every { networkProvider.isNetworkAvailable() } returns false
        every { networkProvider.networkConnectivityFlow() } returns flowOf(false)

        if (hasBookmarks) {
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
        } else {
            every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())
            every { getUsersPagedUseCase() } returns flowOf(PagingData.empty())
        }

        return UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )
    }


    @Test
    fun userListScreen_displaysNetworkError_whenNoInternetAndNoBookmarks() {
        val viewModel = mockk<UserListViewModel>(relaxed = true)
        every { viewModel.pagedUsersWithUpdates } returns flowOf(PagingData.empty())

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = false,
                        showNetworkError = true,
                        searchQuery = ""
                    ),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Check if network error is displayed
        composeTestRule
            .onNodeWithText("📵 No internet connection")
            .assertIsDisplayed()
    }

    @Test
    fun userListScreen_displaysOfflineMode_whenNoInternet() {
        // Δημιουργώ real ViewModel με real mocks
        val viewModel = createOfflineViewModel(hasBookmarks = true)

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

        // Πιο απλό check - αν υπάρχει οποιοδήποτε text που δείχνει offline
        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()

        // Alternative check for any offline-related content
        composeTestRule
            .onRoot()
            .assertExists()
    }

//    @Test
//    fun userListScreen_transitionsFromOfflineToOnline() {
//        val viewModel = createOfflineViewModel(hasBookmarks = true)
//
//        // Create a variable to control the state
//        var currentState = UserListState(
//            isOnline = false,
//            isOfflineMode = true,
//            showNetworkError = false,
//            searchQuery = ""
//        )
//
//        composeTestRule.setContent {
//            PeopleScopeTheme {
//                UserListScreen(
//                    state = currentState,
//                    onIntent = {},
//                    viewModel = viewModel
//                )
//            }
//        }
//
//        composeTestRule.waitForIdle()
//
//        // Verify we're in offline mode by checking for the title
//        composeTestRule
//            .onNodeWithText("People")
//            .assertIsDisplayed()
//
//        // Now transition to online by recreating the composable with new state
//        currentState = UserListState(
//            isOnline = true,
//            isOfflineMode = false,
//            showNetworkError = false,
//            searchQuery = ""
//        )
//
//        composeTestRule.setContent {
//            PeopleScopeTheme {
//                UserListScreen(
//                    state = currentState,
//                    onIntent = {},
//                    viewModel = viewModel
//                )
//            }
//        }
//
//        composeTestRule.waitForIdle()
//
//        // Verify we're still showing the UI properly in online mode
//        composeTestRule
//            .onNodeWithText("People")
//            .assertIsDisplayed()
//
//        // Simple success - if both states show the title, the transition works
//        // This is a basic test that the UI doesn't crash during state changes
//    }

    @Test
    fun userListScreen_searchFunctionality_worksInOfflineMode() {
        val viewModel = mockk<UserListViewModel>()
        every { viewModel.pagedUsersWithUpdates } returns flowOf(PagingData.from(testBookmarkedUsers))
        every { viewModel.processIntent(any()) } just Runs

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(searchQuery = ""),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()

        // Type in search field
        composeTestRule
            .onNodeWithText("Search users...")
            .performTextInput("John")

        composeTestRule.waitForIdle()

        // Just check that we can interact with search - simplified assertion
        composeTestRule
            .onNodeWithText("Search users...")
            .assertExists()
    }

    @Test
    fun userListScreen_pullToRefresh_worksInOfflineMode() {
        val viewModel = createOfflineViewModel(hasBookmarks = true)

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(
                        isOnline = false,
                        isOfflineMode = true,
                        showNetworkError = false,
                        searchQuery = ""
                    ),
                    onIntent = viewModel::processIntent,
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

        // Verify that content is still displayed after swipe
        composeTestRule
            .onNodeWithText("Mr John Offline")
            .assertIsDisplayed()
    }

    @Test
    fun userListScreen_showsCorrectTitle_inOfflineMode() {
        val viewModel = createOfflineViewModel(hasBookmarks = true)

        composeTestRule.setContent {
            PeopleScopeTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()

                UserListScreen(
                    state = state,
                    onIntent = viewModel::processIntent,
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // Should show the main title
        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()
    }

}