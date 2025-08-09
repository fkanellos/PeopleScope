package gr.pkcoding.peoplescope.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.paging.PagingData
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.*
import gr.pkcoding.peoplescope.presentation.ui.userdetail.UserDetailViewModel
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListViewModel
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testUser = User(
        id = "test-navigation-id",
        gender = "male",
        name = Name("Mr", "John", "Navigator"),
        email = "john.navigator@example.com",
        phone = "+1234567890",
        cell = "+0987654321",
        picture = Picture("large.jpg", "medium.jpg", "thumbnail.jpg"),
        location = Location(
            street = Street(123, "Nav St"),
            city = "Navigation City",
            state = "NC",
            country = "Nav Country",
            postcode = "12345",
            coordinates = Coordinates("40.7128", "-74.0060"),
            timezone = Timezone("-5:00", "Eastern Time")
        ),
        dob = DateOfBirth("1990-01-01", 33),
        nationality = "US",
        isBookmarked = false
    )

    private fun createMockedViewModels(): Triple<UserListViewModel, (String) -> UserDetailViewModel, TestNavHostController> {
        // Mock use cases and dependencies
        val getUsersPagedUseCase: GetUsersPagedUseCase = mockk()
        val toggleBookmarkUseCase: ToggleBookmarkUseCase = mockk()
        val bookmarkDao: BookmarkDao = mockk()
        val networkProvider: NetworkConnectivityProvider = mockk()
        val getUserDetailsUseCase: GetUserDetailsUseCase = mockk()
        val isUserBookmarkedUseCase: IsUserBookmarkedUseCase = mockk()

        // Setup mocks
        every { networkProvider.isNetworkAvailable() } returns true
        every { networkProvider.networkConnectivityFlow() } returns flowOf(true)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList<BookmarkedUserEntity>())
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(listOf(testUser)))
        every { getUserDetailsUseCase(any()) } returns Result.Success(testUser)
        every { isUserBookmarkedUseCase(any()) } returns flowOf(false)

        val userListViewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        val createUserDetailViewModel = { userId: String ->
            UserDetailViewModel(
                userId = userId,
                getUserDetailsUseCase = getUserDetailsUseCase,
                toggleBookmarkUseCase = toggleBookmarkUseCase,
                isUserBookmarkedUseCase = isUserBookmarkedUseCase
            )
        }

        val navController = TestNavHostController(composeTestRule.activity).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

        return Triple(userListViewModel, createUserDetailViewModel, navController)
    }

    @Test
    fun navHost_startsAtUserListDestination() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()

        composeTestRule.setContent {
            navController.setCurrentDestination(Destinations.UserList.route)

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        // Verify that we start at the user list destination
        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)
    }

    @Test
    fun userListScreen_navigatesToUserDetail_whenUserClicked() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()

        composeTestRule.setContent {
            navController.setCurrentDestination(Destinations.UserList.route)

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Verify we're on user list screen
        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()

        // Click on a user card
        composeTestRule
            .onNodeWithText("Mr John Navigator")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify navigation occurred
        assertEquals(
            "user_detail/{userId}",
            navController.currentDestination?.route
        )
    }

    @Test
    fun userDetailScreen_navigatesBack_whenBackButtonPressed() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()

        composeTestRule.setContent {
            // Start at user detail screen
            navController.setCurrentDestination(
                Destinations.UserDetail.createRoute(testUser.id!!)
            )

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Verify we're on user detail screen
        composeTestRule
            .onNodeWithText("Profile")
            .assertIsDisplayed()

        // Click back button
        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we navigated back to user list
        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)
    }

    @Test
    fun navigation_handlesUserIdParameter_correctly() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()
        val testUserId = "test-user-123"

        composeTestRule.setContent {
            navController.setCurrentDestination(
                Destinations.UserDetail.createRoute(testUserId)
            )

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Verify that the user detail screen received the correct user ID
        // This would be verified by checking that the correct user data is displayed
        composeTestRule
            .onNodeWithText("Profile")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_fromUserList_toUserDetail_andBack() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()

        composeTestRule.setContent {
            navController.setCurrentDestination(Destinations.UserList.route)

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Start at user list
        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)

        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()

        // Navigate to user detail
        composeTestRule
            .onNodeWithText("Mr John Navigator")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're at user detail
        assertEquals("user_detail/{userId}", navController.currentDestination?.route)

        composeTestRule
            .onNodeWithText("Profile")
            .assertIsDisplayed()

        // Navigate back to user list
        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're back at user list
        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)

        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_handlesInvalidUserId_gracefully() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()

        composeTestRule.setContent {
            // Try to navigate to user detail with empty/invalid user ID
            navController.setCurrentDestination(
                Destinations.UserDetail.createRoute("")
            )

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // The screen should handle the invalid user ID gracefully
        // This might show an error state or navigate back
        composeTestRule
            .onNodeWithText("Profile")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_maintainsState_acrossNavigation() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()

        composeTestRule.setContent {
            navController.setCurrentDestination(Destinations.UserList.route)

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Perform a search on user list
        composeTestRule
            .onNodeWithText("Search users...")
            .performTextInput("John")

        composeTestRule.waitForIdle()

        // Navigate to user detail
        composeTestRule
            .onNodeWithText("Mr John Navigator")
            .performClick()

        composeTestRule.waitForIdle()

        // Navigate back
        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify that the search state is maintained
        composeTestRule
            .onNodeWithText("Search users...")
            .assertTextContains("John")
    }

    @Test
    fun navigation_deepLink_toUserDetail() {
        val (userListViewModel, createUserDetailViewModel, navController) = createMockedViewModels()
        val testUserId = "deep-link-user-id"

        composeTestRule.setContent {
            // Simulate deep link navigation directly to user detail
            navController.setCurrentDestination(
                Destinations.UserDetail.createRoute(testUserId)
            )

            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Verify we're directly at user detail screen
        assertEquals("user_detail/{userId}", navController.currentDestination?.route)

        composeTestRule
            .onNodeWithText("Profile")
            .assertIsDisplayed()

        // Back navigation should work even from deep link
        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .performClick()

        composeTestRule.waitForIdle()

        // Should navigate to user list
        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)
    }
}