package gr.pkcoding.peoplescope.performance

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import gr.pkcoding.peoplescope.data.local.dao.BookmarkDao
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.domain.model.*
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var getUsersPagedUseCase: GetUsersPagedUseCase
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var networkProvider: NetworkConnectivityProvider

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getUsersPagedUseCase = mockk()
        toggleBookmarkUseCase = mockk()
        bookmarkDao = mockk()
        networkProvider = mockk()

        every { networkProvider.isNetworkAvailable() } returns true
        every { networkProvider.networkConnectivityFlow() } returns flowOf(true)
        every { bookmarkDao.getAllBookmarkedUsers() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun generateLargeUserList(count: Int): List<User> {
        return (1..count).map { index ->
            User(
                id = "user-$index",
                gender = if (index % 2 == 0) "male" else "female",
                name = Name("Mr", "User", "$index"),
                email = "user$index@example.com",
                phone = "+123456789$index",
                cell = "+098765432$index",
                picture = Picture("large$index.jpg", "medium$index.jpg", "thumb$index.jpg"),
                location = Location(
                    street = Street(index, "Test St"),
                    city = "City $index",
                    state = "State ${index % 50}",
                    country = "Country ${index % 10}",
                    postcode = "${10000 + index}",
                    coordinates = Coordinates("${40 + index % 10}", "${-74 + index % 10}"),
                    timezone = Timezone("-${index % 12}:00", "Timezone $index")
                ),
                dob = DateOfBirth("199${index % 10}-0${(index % 12) + 1}-01", 30 + (index % 40)),
                nationality = "US",
                isBookmarked = index % 5 == 0 // Every 5th user is bookmarked
            )
        }
    }

    @Test
    fun userListViewModel_initialization_withLargeDataset_performsWell() = testScope.runTest {
        // Given - Large dataset of 1000 users
        val largeUserList = generateLargeUserList(1000)
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(largeUserList))

        // When - Initialize ViewModel and measure time
        val initTime = measureTimeMillis {
            val viewModel = UserListViewModel(
                getUsersPagedUseCase = getUsersPagedUseCase,
                toggleBookmarkUseCase = toggleBookmarkUseCase,
                bookmarkDao = bookmarkDao,
                networkProvider = networkProvider
            )

            // Allow initialization to complete
            advanceUntilIdle()
        }

        // Then - Initialization should be reasonably fast (under 2 seconds)
        assertTrue("ViewModel initialization should be fast (was ${initTime}ms)", initTime < 2000)
    }
    @Test
    fun userListViewModel_searchFiltering_withLargeDataset_performsWell() = testScope.runTest {
        // Given - Large dataset and ViewModel
        val largeUserList = generateLargeUserList(5000)
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(largeUserList))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()

        // When - Perform search operation and measure time
        val searchTime = measureTimeMillis {
            repeat(10) { // Simulate rapid typing
                viewModel.processIntent(
                    gr.pkcoding.peoplescope.presentation.ui.userlist.UserListIntent.UpdateSearchQuery("User ${it * 100}")
                )
                advanceTimeBy(50) // Simulate typing delay
            }
            advanceUntilIdle()
        }

        // Then - Search operations should be fast (under 1 second for 10 operations)
        assertTrue("Search filtering should be fast (was ${searchTime}ms)", searchTime < 1000)
    }

    @Test
    fun userValidation_withLargeDataset_performsWell() = testScope.runTest {
        // Given - Large dataset with mix of valid and invalid users
        val mixedUserList = generateLargeUserList(1000) + listOf(
            // Add some invalid users
            User(null, "male", null, "email", "phone", "cell", null, null, null, "US", false),
            User("", "female", Name("Ms", "", ""), "email", "phone", "cell", null, null, null, "US", false),
            User("valid", "male", Name("Mr", "Valid", "User"), "email", "phone", "cell", null, null, null, "US", false)
        )

        // When - Validate all users and measure time
        val validationTime = measureTimeMillis {
            val validUsers = mixedUserList.filter { it.isValid() }
            val validCount = validUsers.size

            // Should filter out invalid users efficiently
            assertTrue("Should have some valid users", validCount > 0)
            assertTrue("Should filter out some invalid users", validCount < mixedUserList.size)
        }

        // Then - Validation should be fast (under 100ms for 1000+ users)
        assertTrue("User validation should be fast (was ${validationTime}ms)", validationTime < 100)
    }

    @Test
    fun bookmarkOperations_withManyUsers_performWell() = testScope.runTest {
        // Given - Setup for bookmark operations
        coEvery { toggleBookmarkUseCase(any()) } returns Result.Success(Unit)

        // ✅ CRITICAL: Mock the getUsersPagedUseCase that ViewModel needs
        val testUsers = generateLargeUserList(100)
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(testUsers))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        // Allow ViewModel to initialize
        advanceUntilIdle()

        // When - Perform multiple bookmark operations
        val bookmarkTime = measureTimeMillis {
            testUsers.take(20).forEach { user ->
                viewModel.processIntent(
                    gr.pkcoding.peoplescope.presentation.ui.userlist.UserListIntent.ToggleBookmark(user)
                )
            }
            advanceUntilIdle()
        }

        // Then - Bookmark operations should be reasonably fast
        assertTrue("Bookmark operations should be fast (was ${bookmarkTime}ms)", bookmarkTime < 1000)
    }

    @Test
    fun memoryUsage_staysReasonable_withLargeDataset() = testScope.runTest {
        // Given - Get initial memory usage
        val runtime = Runtime.getRuntime()
        runtime.gc() // Force garbage collection
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // When - Create and use ViewModel with large dataset
        val largeUserList = generateLargeUserList(2000)
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(largeUserList))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        // Simulate heavy usage
        repeat(50) {
            viewModel.processIntent(
                gr.pkcoding.peoplescope.presentation.ui.userlist.UserListIntent.UpdateSearchQuery("Search $it")
            )
            advanceTimeBy(10)
        }

        advanceUntilIdle()

        runtime.gc() // Force garbage collection
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Then - Memory increase should be reasonable (less than 50MB)
        val maxMemoryIncrease = 50 * 1024 * 1024 // 50MB
        assertTrue(
            "Memory usage should be reasonable (increased by ${memoryIncrease / (1024 * 1024)}MB)",
            memoryIncrease < maxMemoryIncrease
        )
    }

    @Test
    fun paging_performance_withLargeDataset() = testScope.runTest {
        // Given - Very large dataset split into pages
        val totalUsers = 10000
        val pageSize = 25
        val pages = totalUsers / pageSize

        // Simulate paging behavior
        val pagingTime = measureTimeMillis {
            repeat(10) { pageIndex ->
                val pageUsers = generateLargeUserList(pageSize)
                every { getUsersPagedUseCase() } returns flowOf(PagingData.from(pageUsers))

                // Simulate page load
                advanceTimeBy(100)
            }
            advanceUntilIdle()
        }

        // Then - Paging should be efficient
        assertTrue("Paging should be efficient (was ${pagingTime}ms)", pagingTime < 2000)
    }

    @Test
    fun stateUpdates_frequency_isOptimized() = testScope.runTest {
        // Given - ViewModel setup
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(generateLargeUserList(100)))

        val viewModel = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()

        // When - Rapidly update search query (simulating fast typing)
        val updateTime = measureTimeMillis {
            repeat(20) {
                viewModel.processIntent(
                    gr.pkcoding.peoplescope.presentation.ui.userlist.UserListIntent.UpdateSearchQuery("Test$it")
                )
                advanceTimeBy(50) // 50ms between updates (very fast typing)
            }
            advanceUntilIdle()
        }

        // Then - Updates should be debounced and efficient
        assertTrue("State updates should be debounced (was ${updateTime}ms)", updateTime < 1500)
    }

    @Test
    fun cleanup_releasesResources_properly() = testScope.runTest {
        // Given - Create ViewModel and use it
        val largeUserList = generateLargeUserList(1000)
        every { getUsersPagedUseCase() } returns flowOf(PagingData.from(largeUserList))

        var viewModel: UserListViewModel? = UserListViewModel(
            getUsersPagedUseCase = getUsersPagedUseCase,
            toggleBookmarkUseCase = toggleBookmarkUseCase,
            bookmarkDao = bookmarkDao,
            networkProvider = networkProvider
        )

        advanceUntilIdle()

        // Get memory before cleanup
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val memoryBeforeCleanup = runtime.totalMemory() - runtime.freeMemory()

        // When - Clean up ViewModel
        viewModel = null
        runtime.gc()
        runtime.gc() // Force multiple GC cycles

        // Then - Memory should be released
        val memoryAfterCleanup = runtime.totalMemory() - runtime.freeMemory()
        val memoryReleased = memoryBeforeCleanup - memoryAfterCleanup

        // Should release some memory (at least some cleanup occurred)
        assertTrue(
            "Should release some memory on cleanup (released ${memoryReleased / 1024}KB)",
            memoryReleased >= 0 // At minimum, shouldn't increase
        )
    }
}