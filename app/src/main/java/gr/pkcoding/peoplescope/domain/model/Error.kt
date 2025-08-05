package gr.pkcoding.peoplescope.domain.model

/**
 * Base interface for all errors in the domain layer
 */
sealed interface Error

/**
 * Network-related errors
 */
enum class NetworkError : Error {
    NO_INTERNET,
    REQUEST_TIMEOUT,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN
}

/**
 * Local storage errors
 */
enum class LocalError : Error {
    DISK_FULL,
    DATABASE_ERROR
}

/**
 * Grouped data errors that can come from either network or local sources
 */
sealed interface DataError : Error {
    data class Network(val error: NetworkError) : DataError
    data class Local(val error: LocalError) : DataError
}

/**
 * User-specific domain errors
 */
sealed interface UserError : Error {
    data class UserNotFound(val userId: String) : UserError
}