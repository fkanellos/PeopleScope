package gr.pkcoding.peoplescope.data.mapper

import com.google.gson.JsonSyntaxException
import gr.pkcoding.peoplescope.domain.model.LocalError
import gr.pkcoding.peoplescope.domain.model.NetworkError
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Maps exceptions to NetworkError enum values
 */
fun Exception.toNetworkError(): NetworkError {
    return when (this) {
        is UnknownHostException -> NetworkError.NO_INTERNET
        is SocketTimeoutException -> NetworkError.REQUEST_TIMEOUT
        is TimeoutException -> NetworkError.REQUEST_TIMEOUT
        is TimeoutCancellationException -> NetworkError.REQUEST_TIMEOUT
        is HttpException -> {
            when (code()) {
                408 -> NetworkError.REQUEST_TIMEOUT
                in 500..599 -> NetworkError.SERVER_ERROR
                else -> NetworkError.UNKNOWN
            }
        }
        is JsonSyntaxException -> NetworkError.SERIALIZATION
        else -> NetworkError.UNKNOWN
    }
}

/**
 * Maps exceptions to LocalError enum values
 */
fun Throwable.toLocalError(): LocalError {
    return when (this) {
        is android.database.sqlite.SQLiteFullException -> LocalError.DISK_FULL
        else -> LocalError.DATABASE_ERROR
    }
}

/**
 * Smart network error detection
 */
fun Throwable.isNetworkRelatedError(): Boolean {
    return when (this) {
        // Direct network exception types
        is UnknownHostException,
        is SocketTimeoutException,
        is TimeoutException,
        is TimeoutCancellationException -> true

        // HTTP exceptions that indicate network issues
        is HttpException -> code() in listOf(408, 503, 504, 522, 523, 524)

        // Check error message for network keywords
        else -> {
            val message = (localizedMessage ?: message ?: "").lowercase()
            message.contains("network") ||
                    message.contains("internet") ||
                    message.contains("connection") ||
                    message.contains("timeout") ||
                    message.contains("host") ||
                    message.contains("unreachable")
        }
    }
}