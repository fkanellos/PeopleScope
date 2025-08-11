package gr.pkcoding.peoplescope.presentation.ui.userlist

import gr.pkcoding.peoplescope.domain.model.Coordinates
import gr.pkcoding.peoplescope.domain.model.DateOfBirth
import gr.pkcoding.peoplescope.domain.model.Location
import gr.pkcoding.peoplescope.domain.model.Name
import gr.pkcoding.peoplescope.domain.model.Picture
import gr.pkcoding.peoplescope.domain.model.Street
import gr.pkcoding.peoplescope.domain.model.Timezone
import gr.pkcoding.peoplescope.domain.model.User
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UserListStateTest {

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

    @Test
    fun `shouldShowOfflineContent returns true when offline with cached users`() {
        // Given
        val state = UserListState(
            isOnline = false,
            cachedUsers = listOf(testUser)
        )

        // When & Then
        assertTrue("Should show offline content when has cached users",
            state.shouldShowOfflineContent())
    }

    @Test
    fun `shouldShowOfflineContent returns false when online`() {
        // Given
        val state = UserListState(
            isOnline = true,
            cachedUsers = listOf(testUser)
        )

        // When & Then
        assertFalse("Should not show offline content when online",
            state.shouldShowOfflineContent())
    }

    @Test
    fun `shouldShowOfflineContent returns false when offline with no cached users`() {
        // Given
        val state = UserListState(
            isOnline = false,
            cachedUsers = emptyList()
        )

        // When & Then
        assertFalse("Should not show offline content when no cached users",
            state.shouldShowOfflineContent())
    }

    @Test
    fun `shouldShowNetworkError returns true when offline with no cached users`() {
        // Given
        val state = UserListState(
            isOnline = false,
            cachedUsers = emptyList()
        )

        // When & Then
        assertTrue("Should show network error when offline with no content",
            state.shouldShowNetworkError())
    }

    @Test
    fun `shouldShowNetworkError returns false when offline with cached users`() {
        // Given
        val state = UserListState(
            isOnline = false,
            cachedUsers = listOf(testUser)
        )

        // When & Then
        assertFalse("Should not show network error when has cached content",
            state.shouldShowNetworkError())
    }

    @Test
    fun `shouldShowNetworkError returns false when online`() {
        // Given
        val state = UserListState(
            isOnline = true,
            cachedUsers = emptyList()
        )

        // When & Then
        assertFalse("Should not show network error when online",
            state.shouldShowNetworkError())
    }

    @Test
    fun `isConnectionJustLost returns true when went from online to offline`() {
        // Given
        val state = UserListState(
            isOnline = false,
            lastOnlineState = true
        )

        // When & Then
        assertTrue("Should detect connection just lost",
            state.isConnectionJustLost())
    }

    @Test
    fun `isConnectionJustLost returns false when already offline`() {
        // Given
        val state = UserListState(
            isOnline = false,
            lastOnlineState = false
        )

        // When & Then
        assertFalse("Should not detect connection lost when already offline",
            state.isConnectionJustLost())
    }

    @Test
    fun `isConnectionJustLost returns false when online`() {
        // Given
        val state = UserListState(
            isOnline = true,
            lastOnlineState = false
        )

        // When & Then
        assertFalse("Should not detect connection lost when online",
            state.isConnectionJustLost())
    }

    @Test
    fun `isConnectionJustRestored returns true when went from offline to online`() {
        // Given
        val state = UserListState(
            isOnline = true,
            lastOnlineState = false
        )

        // When & Then
        assertTrue("Should detect connection just restored",
            state.isConnectionJustRestored())
    }

    @Test
    fun `isConnectionJustRestored returns false when already online`() {
        // Given
        val state = UserListState(
            isOnline = true,
            lastOnlineState = true
        )

        // When & Then
        assertFalse("Should not detect connection restored when already online",
            state.isConnectionJustRestored())
    }

    @Test
    fun `isConnectionJustRestored returns false when offline`() {
        // Given
        val state = UserListState(
            isOnline = false,
            lastOnlineState = true
        )

        // When & Then
        assertFalse("Should not detect connection restored when offline",
            state.isConnectionJustRestored())
    }

    @Test
    fun `state transitions work correctly for complete offline to online flow`() {
        // Given - Initial online state
        var state = UserListState(
            isOnline = true,
            lastOnlineState = true,
            cachedUsers = listOf(testUser)
        )

        // Then
        assertFalse("Initially should not show offline content", state.shouldShowOfflineContent())
        assertFalse("Initially should not show network error", state.shouldShowNetworkError())
        assertFalse("Initially should not detect connection lost", state.isConnectionJustLost())
        assertFalse("Initially should not detect connection restored", state.isConnectionJustRestored())

        // When - Connection lost
        state = state.copy(
            isOnline = false,
            lastOnlineState = true // Previous state was online
        )

        // Then
        assertTrue("Should show offline content after losing connection", state.shouldShowOfflineContent())
        assertFalse("Should not show network error when has cached content", state.shouldShowNetworkError())
        assertTrue("Should detect connection just lost", state.isConnectionJustLost())
        assertFalse("Should not detect connection restored", state.isConnectionJustRestored())

        // When - Still offline (state updated)
        state = state.copy(
            lastOnlineState = false // Now previous state is also offline
        )

        // Then
        assertTrue("Should still show offline content", state.shouldShowOfflineContent())
        assertFalse("Should not show network error", state.shouldShowNetworkError())
        assertFalse("Should not detect connection just lost anymore", state.isConnectionJustLost())
        assertFalse("Should not detect connection restored", state.isConnectionJustRestored())

        // When - Connection restored
        state = state.copy(
            isOnline = true,
            lastOnlineState = false // Previous state was offline
        )

        // Then
        assertFalse("Should not show offline content when back online", state.shouldShowOfflineContent())
        assertFalse("Should not show network error when back online", state.shouldShowNetworkError())
        assertFalse("Should not detect connection lost", state.isConnectionJustLost())
        assertTrue("Should detect connection just restored", state.isConnectionJustRestored())
    }
}