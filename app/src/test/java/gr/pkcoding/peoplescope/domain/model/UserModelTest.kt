package gr.pkcoding.peoplescope.domain.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UserModelTest {

    @Test
    fun `User isValid should return true for valid user`() {
        // Given
        val validUser = User(
            id = "valid-id",
            gender = "male",
            name = Name("Mr", "John", "Doe"),
            email = "john@example.com",
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

        // When & Then
        assertTrue("Valid user should return true", validUser.isValid())
    }

    @Test
    fun `User isValid should return false for null id`() {
        // Given
        val invalidUser = User(
            id = null,
            gender = "male",
            name = Name("Mr", "John", "Doe"),
            email = "john@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // When & Then
        assertFalse("User with null id should be invalid", invalidUser.isValid())
    }

    @Test
    fun `User isValid should return false for blank id`() {
        // Given
        val invalidUser = User(
            id = "   ",
            gender = "male",
            name = Name("Mr", "John", "Doe"),
            email = "john@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // When & Then
        assertFalse("User with blank id should be invalid", invalidUser.isValid())
    }

    @Test
    fun `User isValid should return false for null name`() {
        // Given
        val invalidUser = User(
            id = "valid-id",
            gender = "male",
            name = null,
            email = "john@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // When & Then
        assertFalse("User with null name should be invalid", invalidUser.isValid())
    }

    @Test
    fun `User isValid should return false for null first name`() {
        // Given
        val invalidUser = User(
            id = "valid-id",
            gender = "male",
            name = Name("Mr", null, "Doe"),
            email = "john@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // When & Then
        assertFalse("User with null first name should be invalid", invalidUser.isValid())
    }

    @Test
    fun `User isValid should return false for null last name`() {
        // Given
        val invalidUser = User(
            id = "valid-id",
            gender = "male",
            name = Name("Mr", "John", null),
            email = "john@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = null,
            location = null,
            dob = null,
            nationality = "US",
            isBookmarked = false
        )

        // When & Then
        assertFalse("User with null last name should be invalid", invalidUser.isValid())
    }

    @Test
    fun `User getDisplayName should return formatted name`() {
        // Given
        val user = User(
            id = "test-id",
            gender = "male",
            name = Name("Mr", "John", "Doe"),
            email = null,
            phone = null,
            cell = null,
            picture = null,
            location = null,
            dob = null,
            nationality = null,
            isBookmarked = false
        )

        // When
        val displayName = user.getDisplayName()

        // Then
        assertEquals("Mr John Doe", displayName)
    }

    @Test
    fun `User getDisplayName should return Unknown User for null name`() {
        // Given
        val user = User(
            id = "test-id",
            gender = "male",
            name = null,
            email = null,
            phone = null,
            cell = null,
            picture = null,
            location = null,
            dob = null,
            nationality = null,
            isBookmarked = false
        )

        // When
        val displayName = user.getDisplayName()

        // Then
        assertEquals("Unknown User", displayName)
    }

    @Test
    fun `Name getFullName should return formatted name with title`() {
        // Given
        val name = Name("Dr", "Jane", "Smith")

        // When
        val fullName = name.getFullName()

        // Then
        assertEquals("Dr Jane Smith", fullName)
    }

    @Test
    fun `Name getFullName should return formatted name without title`() {
        // Given
        val name = Name(null, "Jane", "Smith")

        // When
        val fullName = name.getFullName()

        // Then
        assertEquals("Jane Smith", fullName)
    }

    @Test
    fun `Name getFullName should filter out blank parts`() {
        // Given
        val name = Name("", "Jane", "   ")

        // When
        val fullName = name.getFullName()

        // Then
        assertEquals("Jane", fullName)
    }

    @Test
    fun `Location getDisplayLocation should return city and country`() {
        // Given
        val location = Location(
            street = Street(123, "Main St"),
            city = "New York",
            state = "NY",
            country = "USA",
            postcode = "10001",
            coordinates = null,
            timezone = null
        )

        // When
        val displayLocation = location.getDisplayLocation()

        // Then
        assertEquals("New York, USA", displayLocation)
    }

    @Test
    fun `Location getDisplayLocation should handle null city`() {
        // Given
        val location = Location(
            street = null,
            city = null,
            state = "NY",
            country = "USA",
            postcode = "10001",
            coordinates = null,
            timezone = null
        )

        // When
        val displayLocation = location.getDisplayLocation()

        // Then
        assertEquals("USA", displayLocation)
    }

    @Test
    fun `Location getDisplayLocation should handle null country`() {
        // Given
        val location = Location(
            street = null,
            city = "New York",
            state = "NY",
            country = null,
            postcode = "10001",
            coordinates = null,
            timezone = null
        )

        // When
        val displayLocation = location.getDisplayLocation()

        // Then
        assertEquals("New York", displayLocation)
    }

    @Test
    fun `Location getDisplayLocation should return empty for null city and country`() {
        // Given
        val location = Location(
            street = null,
            city = null,
            state = "NY",
            country = null,
            postcode = "10001",
            coordinates = null,
            timezone = null
        )

        // When
        val displayLocation = location.getDisplayLocation()

        // Then
        assertEquals("", displayLocation)
    }

    @Test
    fun `User copy should maintain object integrity`() {
        // Given
        val originalUser = User(
            id = "test-id",
            gender = "female",
            name = Name("Ms", "Jane", "Doe"),
            email = "jane@example.com",
            phone = "+1234567890",
            cell = "+0987654321",
            picture = Picture("large.jpg", "medium.jpg", "thumbnail.jpg"),
            location = Location(
                street = Street(456, "Oak St"),
                city = "Los Angeles",
                state = "CA",
                country = "USA",
                postcode = "90210",
                coordinates = Coordinates("34.0522", "-118.2437"),
                timezone = Timezone("-8:00", "Pacific Time")
            ),
            dob = DateOfBirth("1992-05-15", 31),
            nationality = "US",
            isBookmarked = false
        )

        // When
        val copiedUser = originalUser.copy(isBookmarked = true)

        // Then
        assertEquals(originalUser.id, copiedUser.id)
        assertEquals(originalUser.name, copiedUser.name)
        assertEquals(originalUser.email, copiedUser.email)
        assertTrue("Copied user should be bookmarked", copiedUser.isBookmarked)
        assertFalse("Original user should not be bookmarked", originalUser.isBookmarked)
    }
}