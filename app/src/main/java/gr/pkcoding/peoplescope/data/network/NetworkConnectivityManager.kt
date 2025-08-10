package gr.pkcoding.peoplescope.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

interface NetworkConnectivityProvider {
    fun isNetworkAvailable(): Boolean
    fun networkConnectivityFlow(): Flow<Boolean>
}

class NetworkConnectivityManager(
    private val context: Context
) : NetworkConnectivityProvider {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()

    override fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            Timber.w(e, "Error checking network connectivity")
            false // Assume no network on error for safety
        }
    }

    /**
     * Flow that emits network connectivity changes
     */
    override fun networkConnectivityFlow(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        connectivityManager?.registerDefaultNetworkCallback(callback)

        trySend(isNetworkAvailable())

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}