package gr.pkcoding.peoplescope.data.mapper

import com.google.gson.JsonSyntaxException
import gr.pkcoding.peoplescope.domain.model.LocalError
import gr.pkcoding.peoplescope.domain.model.NetworkError
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps exceptions to NetworkError enum values
 */
fun Exception.toNetworkError(): NetworkError {
    return when (this) {
        is UnknownHostException -> NetworkError.NO_INTERNET
        is SocketTimeoutException -> NetworkError.REQUEST_TIMEOUT
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