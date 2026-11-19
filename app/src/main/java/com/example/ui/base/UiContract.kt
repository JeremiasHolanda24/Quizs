package com.example.ui.base

/**
 * Marker interface for immutable UI state snapshots exposed by a [BaseViewModel].
 */
interface UiState

/**
 * Marker interface for user intents/actions dispatched from UI components to a [BaseViewModel].
 */
interface UiEvent

/**
 * Marker interface for one-off side effects (e.g., navigation, snackbars, scroll triggers)
 * emitted by a [BaseViewModel] and consumed once by the UI.
 */
interface UiEffect

/**
 * Type-safe representation of asynchronous operations within a [UiState].
 */
sealed interface AsyncState<out T> {
  data object Idle : AsyncState<Nothing>
  data object Loading : AsyncState<Nothing>
  data class Success<T>(val data: T) : AsyncState<T>
  data class Error(val message: String, val cause: Throwable? = null) : AsyncState<Nothing>
}
