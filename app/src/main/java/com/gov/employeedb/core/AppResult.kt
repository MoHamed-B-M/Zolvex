package com.gov.employeedb.core

/**
 * The data layer's explicit success/failure envelope (PLAN.md §6.3). Excel preview/import/export
 * methods never throw for expected failures — they return [Success] or [Failure] — so callers can
 * branch without try/catch while the operation itself stays free of Android crash handlers.
 */
sealed interface AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    /** Applies [transform] to a success value; failures pass through untouched. */
    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    /** Returns the wrapped value, or `null` on failure. */
    fun getOrNull(): T? = (this as? Success)?.data

    fun fold(onSuccess: (T) -> Unit, onFailure: (AppError) -> Unit) {
        when (this) {
            is Success -> onSuccess(data)
            is Failure -> onFailure(error)
        }
    }
}