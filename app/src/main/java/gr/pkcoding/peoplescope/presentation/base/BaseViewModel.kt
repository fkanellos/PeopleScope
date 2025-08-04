package gr.pkcoding.peoplescope.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI pattern implementation
 * @param S ViewState type
 * @param I ViewIntent type
 * @param E ViewEffect type
 */
abstract class BaseViewModel<S : ViewState, I : ViewIntent, E : ViewEffect>(
    initialState: S
) : ViewModel() {

    // State
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // Intent
    private val _intent = MutableSharedFlow<I>()

    // Effect
    private val _effect = Channel<E>()
    val effect = _effect.receiveAsFlow()

    init {
        subscribeToIntents()
    }

    /**
     * Handle intents
     */
    abstract suspend fun handleIntent(intent: I)

    /**
     * Process intents
     */
    fun processIntent(intent: I) {
        viewModelScope.launch {
            _intent.emit(intent)
        }
    }

    /**
     * Update state
     */
    protected fun updateState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    /**
     * Send effect
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    /**
     * Subscribe to intents
     */
    private fun subscribeToIntents() {
        viewModelScope.launch {
            _intent.collect { intent ->
                handleIntent(intent)
            }
        }
    }
}