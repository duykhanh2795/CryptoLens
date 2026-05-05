package com.cryptolens.app.core.domain.util

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
}

inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) action(data)
    return this
}

inline fun <T> Resource<T>.onError(action: (String) -> Unit): Resource<T> {
    if (this is Resource.Error) action(message)
    return this
}

inline fun <T> Resource<T>.onLoading(action: () -> Unit): Resource<T> {
    if (this is Resource.Loading) action()
    return this
}

fun Double.formatPrice(): String = when {
    this >= 1_000.0  -> "%.2f".format(this)
    this >= 1.0      -> "%.4f".format(this)
    this >= 0.01     -> "%.6f".format(this)
    else             -> "%.8f".format(this)
}

fun Double.formatPercent(): String = "%.2f%%".format(this)

fun Double.formatLargeNumber(): String = when {
    this >= 1_000_000_000 -> "%.2fB".format(this / 1_000_000_000)
    this >= 1_000_000     -> "%.2fM".format(this / 1_000_000)
    this >= 1_000         -> "%.2fK".format(this / 1_000)
    else                  -> "%.2f".format(this)
}
