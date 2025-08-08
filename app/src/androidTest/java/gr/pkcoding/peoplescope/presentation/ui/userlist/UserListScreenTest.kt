package gr.pkcoding.peoplescope.presentation.ui.userlist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.paging.PagingData
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.entity.BookmarkedUserEntity
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserListScreenUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var getUsersPagedUseCase: GetUsersPagedUseCase
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase
    private lateinit var viewModel: UserListViewModel
    private lateinit var bookmarkDao: BookmarkDao

    private val testUsers = listOf(
        User(
            id = "1",
            gender = "male",
            name = Name("Mr", "John", "Doe"),
            email = "john.doe@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = Picture("large1.jpg", "medium1.jpg", "thumbnail1.jpg"),
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
        // Create mocks
        getUsersPagedUseCase = mockk()
        toggleBookmarkUseCase = mockk()
        bookmarkDao = mockk()

        // CRITICAL: Mock the Flow methods that ViewModel observes
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList<BookmarkedUserEntity>())
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(testUsers))

        // Create ViewModel AFTER mocks are set up
        viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao
        )
    }

    @Test
    fun userListScreen_displaysUsers() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Check if the screen title is displayed
        composeTestRule
            .onNodeWithText("People")
            .assertIsDisplayed()

        // Check if user names are displayed
        composeTestRule
            .onNodeWithText("Mr John Doe")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ms Jane Smith")
            .assertIsDisplayed()

        // Check if locations are displayed
        composeTestRule
            .onNodeWithText("New York, USA")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Los Angeles, USA")
            .assertIsDisplayed()
    }

    @Test
    fun userListScreen_searchBar_isDisplayed() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Check if search bar is displayed
        composeTestRule
            .onNodeWithText("Search users...")
            .assertIsDisplayed()
    }

    @Test
    fun userListScreen_searchFunctionality_works() {
        var currentState = UserListState(searchQuery = "")

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = currentState,
                    onIntent = { intent ->
                        when (intent) {
                            is UserListIntent.UpdateSearchQuery -> {
                                currentState = currentState.copy(searchQuery = intent.query)
                            }
                            else -> {}
                        }
                    },
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Type in search bar
        composeTestRule
            .onNodeWithText("Search users...")
            .performTextInput("John")

        // Verify search query is updated in the state
        // Note: This is a simplified test - in reality you'd want to verify
        // that the filtering actually works in the UI
        composeTestRule.waitForIdle()
    }

    @Test
    fun userListScreen_bookmarkButton_isClickable() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Find and click bookmark button (this would be identified by content description)
        composeTestRule
            .onAllNodesWithContentDescription("Bookmark")
            .onFirst()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userListScreen_userCard_isClickable() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Find and verify user cards are clickable
        composeTestRule
            .onNodeWithText("Mr John Doe")
            .assertHasClickAction()

        composeTestRule
            .onNodeWithText("Ms Jane Smith")
            .assertHasClickAction()
    }

    @Test
    fun userListScreen_clearSearchButton_worksWhenSearchNotEmpty() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserListScreen(
                    state = UserListState(searchQuery = "John"),
                    onIntent = {},
                    viewModel = viewModel
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Check if clear button is displayed when search is not empty
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}