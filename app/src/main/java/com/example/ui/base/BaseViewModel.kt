package com.example.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel implementing Unidirectional Data Flow (UDF) with:
 * - [uiState]: hot [StateFlow] of immutable [State] for Compose observation via collectAsStateWithLifecycle().
 * - [uiEffect]: buffered [Flow] of one-off [Effect] events.
 * - [onEvent]: single entry point for UI intents.
 */
abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UiEffect>(
  initialState: State
) : ViewModel() {

  private val _uiState = MutableStateFlow(initialState)
  val uiState: StateFlow<State> = _uiState.asStateFlow()

  private val _uiEffect = Channel<Effect>(Channel.BUFFERED)
  val uiEffect: Flow<Effect> = _uiEffect.receiveAsFlow()

  protected val currentState: State
    get() = _uiState.value

  /**
   * Handles a [UiEvent] dispatched from the UI layer.
   */
  abstract fun onEvent(event: Event)

  /**
   * Atomically updates the current [uiState] using a pure reducer function.
   */
  protected fun updateState(reducer: State.() -> State) {
    _uiState.update { it.reducer() }
  }

  /**
   * Emits a one-off [UiEffect] to be consumed once by the UI.
   */
  protected fun sendEffect(effect: Effect) {
    viewModelScope.launch {
      _uiEffect.send(effect)
    }
  }

  /**
   * Launches a coroutine in [viewModelScope] while preserving structured concurrency
   * by rethrowing [CancellationException] and forwarding other failures to [onError].
   */
  protected fun launchSafely(
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
  ): Job = viewModelScope.launch {
    try {
      block()
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (throwable: Throwable) {
      onError(throwable)
    }
  }

  override fun onCleared() {
    super.onCleared()
    _uiEffect.close()
  }
}
