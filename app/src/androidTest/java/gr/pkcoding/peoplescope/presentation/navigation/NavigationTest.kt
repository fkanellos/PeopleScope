package gr.pkcoding.peoplescope.presentation.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.compose.ComposeNavigator
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SimpleNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createTestNavController(): TestNavHostController {
        return TestNavHostController(composeTestRule.activity).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
    }

    @Test
    fun navHost_initializes_successfully() {
        // ✅ Test that NavHost can be created without crashing
        val navController = createTestNavController()

        composeTestRule.setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        // Should not crash
        composeTestRule.waitForIdle()

        // Should have a current destination eventually
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            navController.currentDestination?.route != null
        }
    }

    @Test
    fun navHost_startsAtUserListDestination() {
        val navController = createTestNavController()

        composeTestRule.setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Wait for NavGraph to set up and navigate to start destination
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            navController.currentDestination?.route == Destinations.UserList.route
        }

        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)
    }

    @Test
    fun programmatic_navigation_toUserDetail_works() {
        val navController = createTestNavController()

        composeTestRule.setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Wait for initial setup
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            navController.currentDestination?.route == Destinations.UserList.route
        }

        // Programmatically navigate to user detail
        val testUserId = "test-user-123"
        composeTestRule.runOnUiThread {
            navController.navigate(Destinations.UserDetail.createRoute(testUserId))
        }

        composeTestRule.waitForIdle()

        // Verify navigation succeeded
        assertEquals("user_detail/{userId}", navController.currentDestination?.route)

        // Verify user ID argument was passed
        val userIdArg = navController.currentBackStackEntry?.arguments?.getString("userId")
        assertEquals(testUserId, userIdArg)
    }

    @Test
    fun navigation_backFromUserDetail_works() {
        val navController = createTestNavController()

        composeTestRule.setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Wait for initial setup
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            navController.currentDestination?.route == Destinations.UserList.route
        }

        // Navigate to detail
        composeTestRule.runOnUiThread {
            navController.navigate(Destinations.UserDetail.createRoute("test-user"))
        }

        composeTestRule.waitForIdle()

        // Verify we're at detail
        assertEquals("user_detail/{userId}", navController.currentDestination?.route)

        // Navigate back
        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        composeTestRule.waitForIdle()

        // Verify we're back at user list
        assertEquals(Destinations.UserList.route, navController.currentDestination?.route)
    }

    @Test
    fun destinations_routes_areCorrect() {
        // ✅ Simple test to verify route constants
        assertEquals("user_list", Destinations.UserList.route)
        assertEquals("user_detail/{userId}", Destinations.UserDetail.route)

        // Test route creation
        val testUserId = "abc123"
        val createdRoute = Destinations.UserDetail.createRoute(testUserId)
        assertEquals("user_detail/abc123", createdRoute)
    }

    @Test
    fun multiple_navigation_operations_work() {
        val navController = createTestNavController()

        composeTestRule.setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Wait for initial setup
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            navController.currentDestination?.route == Destinations.UserList.route
        }

        val userIds = listOf("user1", "user2", "user3")

        userIds.forEach { userId ->
            // Navigate to detail
            composeTestRule.runOnUiThread {
                navController.navigate(Destinations.UserDetail.createRoute(userId))
            }

            composeTestRule.waitForIdle()

            // Verify navigation
            assertEquals("user_detail/{userId}", navController.currentDestination?.route)
            assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userId"))

            // Navigate back
            composeTestRule.runOnUiThread {
                navController.popBackStack()
            }

            composeTestRule.waitForIdle()

            // Verify we're back at list
            assertEquals(Destinations.UserList.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun navigation_argumentPassing_works() {
        val navController = createTestNavController()

        composeTestRule.setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph(navController = navController)
            }
        }

        composeTestRule.waitForIdle()

        // Wait for initial setup
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            navController.currentDestination?.route == Destinations.UserList.route
        }

        val testCases = listOf(
            "simple-user-id",
            "user-with-dashes",
            "user123",
            "user_with_underscores"
        )

        testCases.forEach { userId ->
            // Navigate with specific user ID
            composeTestRule.runOnUiThread {
                navController.navigate(Destinations.UserDetail.createRoute(userId))
            }

            composeTestRule.waitForIdle()

            // Verify argument passing
            assertEquals("user_detail/{userId}", navController.currentDestination?.route)
            assertEquals(userId, navController.currentBackStackEntry?.arguments?.getString("userId"))

            // Navigate back for next test
            composeTestRule.runOnUiThread {
                navController.popBackStack()
            }

            composeTestRule.waitForIdle()
        }
    }
}