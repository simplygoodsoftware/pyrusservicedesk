package com.pyrus.pyrusservicedesk._ref.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Try2<out T, E> {
    class Success<T, E>(val value: T) : Try2<T, E>()
    class Failure<E>(val error: E) : Try2<Nothing, E>()

    override fun toString(): String {
        return when (this) {
            is Success -> "Success[value=$value]"
            is Failure -> "Failure[error=$error]"
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun <T, E> Try2<T, E>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Try2.Success)
        returns(false) implies (this@isSuccess is Try2.Failure)
    }
    return this is Try2.Success
}

@OptIn(ExperimentalContracts::class)
fun <T, E> Try2<T, E>.isFailed(): Boolean {
    contract {
        returns(false) implies (this@isFailed is Try2.Success)
        returns(true) implies (this@isFailed is Try2.Failure)
    }
    return this is Try2.Failure<E>
}

fun <T, E> Try.Success<T>.toTry2(): Try2.Success<T, E> {
    return Try2.Success(value)
}

fun <E> Try.Failure.toTry2(transformError: (Throwable) -> E,): Try2.Failure<E> {
    return Try2.Failure(transformError(error))
}

fun <T, T2, E> Try<T>.mapToTry2(
    transform: (T) -> T2,
    transformError: (Throwable) -> E,
): Try2<T2, E> = when (this) {
    is Try.Success -> Try2.Success(transform(value))
    is Try.Failure -> Try2.Failure(transformError(error))
}

fun <T, E> Try<T>.mapToTry2(
    transformError: (Throwable) -> E,
): Try2<T, E> = when (this) {
    is Try.Success -> Try2.Success(value)
    is Try.Failure -> Try2.Failure(transformError(error))
}

fun <T, E, E2> Try2.Success<T, E>.map(): Try2<T, E2> = Try2.Success(value)

fun <E, E2> Try2.Failure<E>.map(transform: (E) -> E2): Try2.Failure<E2> = Try2.Failure(transform(error))

fun <T, T2, E> Try2<T, E>.map(transform: (T) -> T2): Try2<T2, E> = when (this) {
    is Try2.Success -> Try2.Success(transform(value))
    is Try2.Failure -> Try2.Failure(error)
}

fun <T, T2, E, E2> Try2<T, E>.map(
    transform: (T) -> T2,
    transformError: (E) -> E2,
): Try2<T2, E2> = when (this) {
    is Try2.Success -> Try2.Success(transform(value))
    is Try2.Failure -> Try2.Failure(transformError(error))
}

fun <T, T2, E> Try2<T, E>.next(transform: (T) -> Try2<T2, E>): Try2<T2, E> = when (this) {
    is Try2.Success -> transform(value)
    is Try2.Failure -> Try2.Failure(error)
}


/**
 * Return [Try.Success.value] or `null`
 */
fun <T : Any?, E> Try2<T, E>.getOrNull(): T? = (this as? Try2.Success)?.value

/**
 * Return [Try.Failure.error] or `null`
 */
fun <T : Any?, E> Try2<T, E>.getErrorOrNull(): E? = (this as? Try2.Failure)?.error

/**
 * Return [Try.Success.value] or [default]
 */
fun <T : Any?, E> Try2<T, E>.getOrDefault(default: T): T = getOrNull() ?: default
