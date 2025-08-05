package gr.pkcoding.peoplescope.presentation

import gr.pkcoding.peoplescope.R
import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.LocalError
import gr.pkcoding.peoplescope.domain.model.NetworkError
import gr.pkcoding.peoplescope.domain.model.UserError
/**
 * Maps NetworkError to UiText for display
 */
fun NetworkError.toUiText(): UiText {
    return when (this) {
        NetworkError.NO_INTERNET -> UiText.StringResource(R.string.error_no_internet)
        NetworkError.REQUEST_TIMEOUT -> UiText.StringResource(R.string.error_timeout)
        NetworkError.SERVER_ERROR -> UiText.StringResource(R.string.error_server)
        NetworkError.SERIALIZATION -> UiText.StringResource(R.string.error_parsing)
        NetworkError.UNKNOWN -> UiText.StringResource(R.string.error_unknown)
    }
}

/**
 * Maps LocalError to UiText for display
 */
fun LocalError.toUiText(): UiText {
    return when (this) {
        LocalError.DISK_FULL -> UiText.StringResource(R.string.error_disk_full)
        LocalError.DATABASE_ERROR -> UiText.StringResource(R.string.error_database)
    }
}

/**
 * Maps DataError to UiText for display
 */
fun DataError.toUiText(): UiText {
    return when (this) {
        is DataError.Network -> error.toUiText()
        is DataError.Local -> error.toUiText()
    }
}

/**
 * Maps UserError to UiText for display
 */
fun UserError.toUiText(): UiText {
    return when (this) {
        is UserError.UserNotFound -> UiText.StringResource(R.string.error_user_not_found, userId)
    }
}