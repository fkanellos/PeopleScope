package gr.pkcoding.peoplescope.domain.model

/**
 * Custom Result wrapper for handling success and error states
 * @param D The type of data in case of success
 * @param E The type of error, must extend Error interface
 */
sealed class Result<out D, out E : Error> {
    /**
     * Successful result containing data
     */
    data class Success<out D>(val data: D) : Result<D, Nothing>()

    /**
     * Error result containing error information
     */
    data class Error<out E : gr.pkcoding.peoplescope.domain.model.Error>(
        val error: E
    ) : Result<Nothing, E>()

    /**
     * Returns true if this is a Success
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Get data or null
     */
    fun getOrNull(): D? = when (this) {
        is Success -> data
        is Error -> null
    }
}