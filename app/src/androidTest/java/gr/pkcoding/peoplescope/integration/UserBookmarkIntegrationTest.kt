package gr.pkcoding.peoplescope.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.local.database.AppDatabase
import gr.pkcoding.peoplescope.data.mapper.toBookmarkedEntity
import gr.pkcoding.peoplescope.data.mapper.toDomainModel
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.data.repository.UserRepositoryImpl
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserBookmarkIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var api: RandomUserApi
    private lateinit var networkProvider: NetworkConnectivityProvider
    private lateinit var repository: UserRepositoryImpl
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase

    private val testUser = User(
        id = "integration-test-id",
        gender = "female",
        name = Name("Dr", "Jane", "Integration"),
        email = "jane.integration@example.com",
        phone = "+1987654321",
        cell = "+1234567890",
        picture = Picture("large.jpg", "medium.jpg", "thumbnail.jpg"),
        location = Location(
            street = Street(789, "Integration Blvd"),
            city = "Test City",
            state = "TC",
            country = "Test Country",
            postcode = "54321",
            coordinates = Coordinates("41.8781", "-87.6298"),
            timezone = Timezone("-6:00", "Central Time")
        ),
        dob = DateOfBirth("1988-03-22T00:00:00.000Z", 36),
        nationality = "US",
        isBookmarked = false
    )

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        bookmarkDao = database.bookmarkDao()

        // Mock API and network provider
        api = mockk()
        networkProvider = mockk()
        every { networkProvider.isNetworkAvailable() } returns true

        // Create real repository and use case instances
        repository = UserRepositoryImpl(api, bookmarkDao, networkProvider)
        toggleBookmarkUseCase = ToggleBookmarkUseCase(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bookmarkUser_savesToDatabase_andCanBeRetrieved() = runTest {
        // Given - User is not bookmarked initially
        assertFalse("User should not be bookmarked initially", testUser.isBookmarked)

        // When - Bookmark the user
        val bookmarkResult = repository.bookmarkUser(testUser)

        // Then - Bookmark operation should succeed
        assertTrue("Bookmark operation should succeed", bookmarkResult.isSuccess())

        // And - User should be retrievable from database
        val savedUser = bookmarkDao.getBookmarkedUserById(testUser.id!!)
        assertNotNull("User should be saved in database", savedUser)
        assertEquals("Saved user ID should match", testUser.id, savedUser!!.id)
        assertEquals("Saved user first name should match", testUser.name?.first, savedUser.firstName)
        assertEquals("Saved user last name should match", testUser.name?.last, savedUser.lastName)
        assertEquals("Saved user email should match", testUser.email, savedUser.email)

        // And - User should be marked as bookmarked when converted back to domain model
        val retrievedUser = savedUser.toDomainModel()
        assertTrue("Retrieved user should be bookmarked", retrievedUser.isBookmarked)
    }

    @Test
    fun toggleBookmark_addsBookmark_whenUserNotBookmarked() = runTest {
        // Given - User is not bookmarked
        val userNotBookmarked = testUser.copy(isBookmarked = false)

        // When - Toggle bookmark
        val result = toggleBookmarkUseCase(userNotBookmarked)

        // Then - Operation should succeed
        assertTrue("Toggle bookmark should succeed", result.isSuccess())

        // And - User should now be bookmarked in database
        val bookmarkedUser = bookmarkDao.getBookmarkedUserById(testUser.id!!)
        assertNotNull("User should be bookmarked", bookmarkedUser)
        assertEquals("Bookmarked user ID should match", testUser.id, bookmarkedUser!!.id)
    }

    @Test
    fun toggleBookmark_removesBookmark_whenUserIsBookmarked() = runTest {
        // Given - User is already bookmarked
        val entity = testUser.toBookmarkedEntity()!!
        bookmarkDao.insertBookmarkedUser(entity)

        // Verify user is bookmarked
        val bookmarkedUser = bookmarkDao.getBookmarkedUserById(testUser.id!!)
        assertNotNull("User should be bookmarked initially", bookmarkedUser)

        // When - Toggle bookmark with bookmarked user
        val userBookmarked = testUser.copy(isBookmarked = true)
        val result = toggleBookmarkUseCase(userBookmarked)

        // Then - Operation should succeed
        assertTrue("Toggle bookmark should succeed", result.isSuccess())

        // And - User should no longer be bookmarked in database
        val removedUser = bookmarkDao.getBookmarkedUserById(testUser.id!!)
        assertNull("User should no longer be bookmarked", removedUser)
    }

    @Test
    fun bookmarkStatus_isObservable_throughFlow() = runTest {
        // Given - User is not bookmarked initially
        val isBookmarkedFlow = repository.isUserBookmarked(testUser.id!!)

        // Initially should not be bookmarked
        assertFalse("User should not be bookmarked initially", isBookmarkedFlow.first())

        // When - Bookmark the user
        repository.bookmarkUser(testUser)

        // Then - Flow should emit true
        assertTrue("User should be bookmarked", isBookmarkedFlow.first())

        // When - Remove bookmark
        repository.removeBookmark(testUser.id!!)

        // Then - Flow should emit false
        assertFalse("User should not be bookmarked after removal", isBookmarkedFlow.first())
    }

    @Test
    fun multipleUsers_canBeBookmarked_andRetrieved() = runTest {
        // Given - Multiple users
        val user1 = testUser.copy(id = "user-1", name = Name("Mr", "John", "One"))
        val user2 = testUser.copy(id = "user-2", name = Name("Ms", "Jane", "Two"))
        val user3 = testUser.copy(id = "user-3", name = Name("Dr", "Bob", "Three"))

        // When - Bookmark all users
        repository.bookmarkUser(user1)
        repository.bookmarkUser(user2)
        repository.bookmarkUser(user3)

        // Then - All users should be retrievable
        val allBookmarked = bookmarkDao.getAllBookmarkedUsers().first()
        assertEquals("Should have 3 bookmarked users", 3, allBookmarked.size)

        // And - Each user should be correctly stored
        val user1Saved = bookmarkDao.getBookmarkedUserById("user-1")
        val user2Saved = bookmarkDao.getBookmarkedUserById("user-2")
        val user3Saved = bookmarkDao.getBookmarkedUserById("user-3")

        assertNotNull("User 1 should be saved", user1Saved)
        assertNotNull("User 2 should be saved", user2Saved)
        assertNotNull("User 3 should be saved", user3Saved)

        assertEquals("User 1 name should match", "One", user1Saved!!.lastName)
        assertEquals("User 2 name should match", "Two", user2Saved!!.lastName)
        assertEquals("User 3 name should match", "Three", user3Saved!!.lastName)
    }

    @Test
    fun bookmarkData_persistsAcrossRepositoryInstances() = runTest {
        // Given - Bookmark a user with first repository instance
        repository.bookmarkUser(testUser)

        // Verify bookmark exists
        val savedUser = bookmarkDao.getBookmarkedUserById(testUser.id!!)
        assertNotNull("User should be bookmarked", savedUser)

        // When - Create new repository instance (simulating app restart)
        val newRepository = UserRepositoryImpl(api, bookmarkDao, networkProvider)

        // Then - Bookmark should still exist and be observable
        val isBookmarked = newRepository.isUserBookmarked(testUser.id!!).first()
        assertTrue("User should still be bookmarked", isBookmarked)

        // And - User details should be retrievable
        val userResult = newRepository.getUserById(testUser.id!!)
        assertTrue("User retrieval should succeed", userResult.isSuccess())

        val retrievedUser = userResult.getOrNull()
        assertNotNull("Retrieved user should not be null", retrievedUser)
        assertTrue("Retrieved user should be marked as bookmarked", retrievedUser!!.isBookmarked)
        assertEquals("Retrieved user data should match", testUser.name?.first, retrievedUser.name?.first)
    }

    @Test
    fun bookmarkTimestamp_isRecorded_correctly() = runTest {
        // Given - Record time before bookmarking
        val beforeBookmark = System.currentTimeMillis()

        // When - Bookmark user
        repository.bookmarkUser(testUser)

        // Then - Bookmark timestamp should be recent
        val savedUser = bookmarkDao.getBookmarkedUserById(testUser.id!!)
        assertNotNull("User should be saved", savedUser)

        val bookmarkTime = savedUser!!.bookmarkedAt
        assertTrue("Bookmark time should be after start time", bookmarkTime >= beforeBookmark)
        assertTrue("Bookmark time should be reasonable", bookmarkTime <= System.currentTimeMillis())
    }

    @Test
    fun invalidUser_cannotBeBookmarked() = runTest {
        // Given - Invalid user (null ID)
        val invalidUser = testUser.copy(id = null)

        // When - Try to bookmark invalid user
        val result = repository.bookmarkUser(invalidUser)

        // Then - Operation should fail
        assertTrue("Bookmark should fail for invalid user", result is Result.Error)

        // And - No data should be saved
        val allBookmarked = bookmarkDao.getAllBookmarkedUsers().first()
        assertTrue("No users should be bookmarked", allBookmarked.isEmpty())
    }

    @Test
    fun bookmarkRemoval_cleansUpCompletelyFromDatabase() = runTest {
        // Given - User is bookmarked
        repository.bookmarkUser(testUser)

        // Verify bookmark exists
        assertNotNull("User should be bookmarked", bookmarkDao.getBookmarkedUserById(testUser.id!!))
        assertTrue("User should be marked as bookmarked", repository.isUserBookmarked(testUser.id!!).first())

        // When - Remove bookmark
        val result = repository.removeBookmark(testUser.id!!)

        // Then - Operation should succeed
        assertTrue("Bookmark removal should succeed", result.isSuccess())

        // And - User should be completely removed from database
        assertNull("User should not exist in database", bookmarkDao.getBookmarkedUserById(testUser.id!!))
        assertFalse("User should not be marked as bookmarked", repository.isUserBookmarked(testUser.id!!).first())

        // And - No trace should remain in bookmarked users list
        val allBookmarked = bookmarkDao.getAllBookmarkedUsers().first()
        assertTrue("No users should be bookmarked", allBookmarked.isEmpty())
    }
}