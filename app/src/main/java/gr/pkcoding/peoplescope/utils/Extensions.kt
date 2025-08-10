package gr.pkcoding.peoplescope.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import gr.pkcoding.peoplescope.domain.model.Error
import gr.pkcoding.peoplescope.domain.model.Result
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Extension function to collect Flow safely in Composables
 */
@Composable
fun <T> Flow<T>.CollectAsEffect(
    key: Any? = null,
    block: suspend (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(key ?: this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(block)
        }
    }
}

/**
 * Format date string
 */
fun String.formatDate(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(this)
        date?.let { outputFormat.format(it) } ?: this
    } catch (_: Exception) {
        this
    }
}


/**
 * Show toast message
 */
fun Context.showToast(
    message: String,
    duration: Int = Toast.LENGTH_SHORT
) {
    try {
        Toast.makeText(this, message, duration).show()
    } catch (e: Exception) {
        Timber.w(e, "Failed to show toast: $message")
    }
}

@OptIn(FlowPreview::class)
fun <T> Flow<T>.debounceSearch(timeoutMillis: Long = Constants.SEARCH_DEBOUNCE_MS): Flow<T> {
    return this.debounce(timeoutMillis)
}

fun Activity.enableImmersiveMode() {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    window.decorView.post {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}

// Extension function for Result
inline fun <T, E : Error> Result<T, E>.fold(
    onSuccess: (T) -> Unit,
    onError: (E) -> Unit
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(error)
    }
}
