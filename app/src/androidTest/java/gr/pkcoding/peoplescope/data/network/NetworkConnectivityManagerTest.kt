//package gr.pkcoding.peoplescope.data.network
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import io.mockk.*
//import kotlinx.coroutines.test.runTest
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//
//@RunWith(AndroidJUnit4::class)
//class NetworkConnectivityManagerTest {
//
//    private lateinit var context: Context
//    private lateinit var connectivityManager: ConnectivityManager
//    private lateinit var network: Network
//    private lateinit var networkCapabilities: NetworkCapabilities
//    private lateinit var networkConnectivityManager: NetworkConnectivityManager
//
//    @Before
//    fun setup() {
//        context = mockk(relaxed = true)
//        connectivityManager = mockk(relaxed = true)
//        network = mockk(relaxed = true)
//        networkCapabilities = mockk(relaxed = true)
//
//        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
//
//        networkConnectivityManager = NetworkConnectivityManager(context)
//    }
//
//    @Test
//    fun `isNetworkAvailable should return true when network has internet capability`() {
//        // Given
//        every { connectivityManager.activeNetwork } returns network
//        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
//        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
//
//        // When
//        val result = networkConnectivityManager.isNetworkAvailable()
//
//        // Then
//        assertTrue("Should return true when network has internet capability", result)
//    }
//
//    @Test
//    fun `isNetworkAvailable should return false when network has no internet capability`() {
//        // Given
//        every { connectivityManager.activeNetwork } returns network
//        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
//        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
//
//        // When
//        val result = networkConnectivityManager.isNetworkAvailable()
//
//        // Then
//        assertFalse("Should return false when network has no internet capability", result)
//    }
//
//    @Test
//    fun `isNetworkAvailable should return false when no active network`() {
//        // Given
//        every { connectivityManager.activeNetwork } returns null
//
//        // When
//        val result = networkConnectivityManager.isNetworkAvailable()
//
//        // Then
//        assertFalse("Should return false when no active network", result)
//    }
//
//    @Test
//    fun `isNetworkAvailable should return false when connectivity manager is null`() {
//        // Given
//        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null
//        val managerWithNullContext = NetworkConnectivityManager(context)
//
//        // When
//        val result = managerWithNullContext.isNetworkAvailable()
//
//        // Then
//        assertFalse("Should return false when connectivity manager is null", result)
//    }
//
//    @Test
//    fun `isNetworkAvailable should return false when network capabilities are null`() {
//        // Given
//        every { connectivityManager.activeNetwork } returns network
//        every { connectivityManager.getNetworkCapabilities(network) } returns null
//
//        // When
//        val result = networkConnectivityManager.isNetworkAvailable()
//
//        // Then
//        assertFalse("Should return false when network capabilities are null", result)
//    }
//
//    @Test
//    fun `isNetworkAvailable should return false when exception is thrown`() {
//        // Given
//        every { connectivityManager.activeNetwork } throws SecurityException("Permission denied")
//
//        // When
//        val result = networkConnectivityManager.isNetworkAvailable()
//
//        // Then
//        assertFalse("Should return false when exception is thrown", result)
//    }
//
//    @Test
//    fun `networkConnectivityFlow should emit current state immediately`() = runTest {
//        // Given
//        every { connectivityManager.activeNetwork } returns network
//        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
//        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
//
//        // Mock callback registration
//        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
//        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just Runs
//        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
//
//        // When & Then
//        networkConnectivityManager.networkConnectivityFlow().test {
//            // Should emit current state immediately
//            assertEquals(true, awaitItem())
//            cancelAndIgnoreRemainingEvents()
//        }
//
//        verify { connectivityManager.registerDefaultNetworkCallback(any()) }
//    }
//
//    @Test
//    fun `networkConnectivityFlow should emit false when network is lost`() = runTest {
//        // Given
//        every { connectivityManager.activeNetwork } returns network
//        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
//        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
//
//        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
//        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just Runs
//        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
//
//        // When & Then
//        networkConnectivityManager.networkConnectivityFlow().test {
//            // Initial state
//            assertEquals(true, awaitItem())
//
//            // Simulate network loss
//            callbackSlot.captured.onLost(network)
//            assertEquals(false, awaitItem())
//
//            cancelAndIgnoreRemainingEvents()
//        }
//    }
//
//    @Test
//    fun `networkConnectivityFlow should emit true when network becomes available`() = runTest {
//        // Given
//        every { connectivityManager.activeNetwork } returns null
//        every { connectivityManager.getNetworkCapabilities(null) } returns null
//
//        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
//        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just Runs
//        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
//
//        // When & Then
//        networkConnectivityManager.networkConnectivityFlow().test {
//            // Initial state (offline)
//            assertEquals(false, awaitItem())
//
//            // Simulate network becoming available
//            callbackSlot.captured.onAvailable(network)
//            assertEquals(true, awaitItem())
//
//            cancelAndIgnoreRemainingEvents()
//        }
//    }
//
//    @Test
//    fun `networkConnectivityFlow should handle multiple state changes`() = runTest {
//        // Given
//        every { connectivityManager.activeNetwork } returns network
//        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
//        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
//
//        val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
//        every { connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot)) } just Runs
//        every { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just Runs
//
//        // When & Then
//        networkConnectivityManager.networkConnectivityFlow().test {
//            // Initial state
//            assertEquals(true, awaitItem())
//
//            // Network lost
//            callbackSlot.captured.onLost(network)
//            assertEquals(false, awaitItem())
//
//            // Network available again
//            callbackSlot.captured.onAvailable(network)
//            assertEquals(true, awaitItem())
//
//            // Network lost again
//            callbackSlot.captured.onLost(network)
//            assertEquals(false, awaitItem())
//
//            cancelAndIgnoreRemainingEvents()
//        }
//    }
//}