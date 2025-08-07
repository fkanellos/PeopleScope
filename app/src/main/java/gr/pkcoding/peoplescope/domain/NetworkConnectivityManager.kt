package gr.pkcoding.peoplescope.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import gr.pkcoding.peoplescope.domain.model.DataError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

class NetworkConnectivityManager(private val context: Context) {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            Timber.w(e, "Error checking network connectivity")
            false // Assume no network on error για safety
        }
    }

    /**
     * Flow that emits network connectivity changes
     */
    fun networkConnectivityFlow(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        connectivityManager?.registerDefaultNetworkCallback(callback)

        // Send current state
        trySend(isNetworkAvailable())

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}