package gr.pkcoding.peoplescope.presentation.ui.userdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.presentation.UiText
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import org.junit.Rule
import org.junit.Test

class UserDetailScreenUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testUser = User(
        id = "test-id",
        gender = "female",
        name = Name("Dr", "Jane", "Smith"),
        email = "jane.smith@example.com",
        phone = "+1987654321",
        cell = "+1234567890",
        picture = Picture("large.jpg", "medium.jpg", "thumbnail.jpg"),
        location = Location(
            street = Street(456, "Oak Ave"),
            city = "Los Angeles",
            state = "CA",
            country = "USA",
            postcode = "90210",
            coordinates = Coordinates("34.0522", "-118.2437"),
            timezone = Timezone("-8:00", "Pacific Time")
        ),
        dob = DateOfBirth("1992-05-15T00:00:00.000Z", 31),
        nationality = "US",
        isBookmarked = false
    )

    @Test
    fun userDetailScreen_displaysUserInformation() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if user name is displayed
        composeTestRule
            .onNodeWithText("Dr Jane Smith")
            .assertIsDisplayed()

        // Check if email is displayed
        composeTestRule
            .onNodeWithText("jane.smith@example.com")
            .assertIsDisplayed()

        // Check if location is displayed
        composeTestRule
            .onNodeWithText("Los Angeles, USA")
            .assertIsDisplayed()

        // Check if age is displayed
        composeTestRule
            .onNodeWithText("31 years old")
            .assertIsDisplayed()

        // Check if nationality is displayed
        composeTestRule
            .onNodeWithText("US")
            .assertIsDisplayed()
    }

    @Test
    fun userDetailScreen_showsBookmarkButton() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if bookmark button is displayed and clickable
        composeTestRule
            .onNodeWithContentDescription("Bookmark")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userDetailScreen_bookmarkButton_changesState() {
        var currentState = UserDetailState(
            user = testUser,
            isBookmarked = false,
            isLoading = false,
            error = null
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = currentState,
                    onIntent = { intent ->
                        if (intent is UserDetailIntent.ToggleBookmark) {
                            currentState = currentState.copy(isBookmarked = !currentState.isBookmarked)
                        }
                    },
                    onNavigateBack = {}
                )
            }
        }

        // Initially should show bookmark (not bookmarked)
        composeTestRule
            .onNodeWithContentDescription("Bookmark")
            .assertIsDisplayed()

        // Click bookmark button
        composeTestRule
            .onNodeWithContentDescription("Bookmark")
            .performClick()

        // Wait for state change
        composeTestRule.waitForIdle()

        // Should now show unbookmark (bookmarked)
        composeTestRule
            .onNodeWithContentDescription("Remove Bookmark")
            .assertIsDisplayed()
    }

    @Test
    fun userDetailScreen_backButton_isDisplayed() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if back button is displayed
        composeTestRule
            .onNodeWithContentDescription("Navigate back")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userDetailScreen_showsLoadingState() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = null,
                        isBookmarked = false,
                        isLoading = true,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if loading indicator is displayed
        composeTestRule
            .onNode(hasTestTag("loading") or hasContentDescription("Loading"))
            .assertExists()
    }

    @Test
    fun userDetailScreen_showsErrorState() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = null,
                        isBookmarked = false,
                        isLoading = false,
                        error = UiText.DynamicString("User not found")
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if error message is displayed
        composeTestRule
            .onNodeWithText("User not found")
            .assertIsDisplayed()

        // Check if retry button is displayed
        composeTestRule
            .onNodeWithText("Retry")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userDetailScreen_copyEmailButton_isDisplayed() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if copy email button is displayed
        composeTestRule
            .onNodeWithContentDescription("Copy Email")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userDetailScreen_copyPhoneButton_isDisplayed() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if copy phone button is displayed
        composeTestRule
            .onNodeWithContentDescription("Copy Phones")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun userDetailScreen_displaysContactInformation() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if contact information section is displayed
        composeTestRule
            .onNodeWithText("Contact Information")
            .assertIsDisplayed()

        // Check if email label is displayed
        composeTestRule
            .onNodeWithText("Email")
            .assertIsDisplayed()

        // Check if phone label is displayed
        composeTestRule
            .onNodeWithText("Phones")
            .assertIsDisplayed()
    }

    @Test
    fun userDetailScreen_displaysLocationInformation() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if location section is displayed
        composeTestRule
            .onNodeWithText("Location")
            .assertIsDisplayed()

        // Check if nationality label is displayed
        composeTestRule
            .onNodeWithText("Nationality")
            .assertIsDisplayed()
    }

    @Test
    fun userDetailScreen_displaysAdditionalInformation() {
        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = testUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Check if additional information section is displayed
        composeTestRule
            .onNodeWithText("Additional Information")
            .assertIsDisplayed()

        // Check if date of birth label is displayed
        composeTestRule
            .onNodeWithText("Date of Birth")
            .assertIsDisplayed()

        // Check if timezone label is displayed
        composeTestRule
            .onNodeWithText("Timezone")
            .assertIsDisplayed()
    }

    @Test
    fun userDetailScreen_handlesUserWithMinimalData() {
        val minimalUser = User(
            id = "minimal-id",
            gender = null,
            name = Name(null, "John", "Doe"),
            email = null,
            phone = null,
            cell = null,
            picture = null,
            location = null,
            dob = null,
            nationality = null,
            isBookmarked = false
        )

        composeTestRule.setContent {
            PeopleScopeTheme {
                UserDetailScreen(
                    state = UserDetailState(
                        user = minimalUser,
                        isBookmarked = false,
                        isLoading = false,
                        error = null
                    ),
                    onIntent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Should still display the name
        composeTestRule
            .onNodeWithText("John Doe")
            .assertIsDisplayed()

        // Should display bookmark button
        composeTestRule
            .onNodeWithContentDescription("Bookmark")
            .assertIsDisplayed()
    }
}