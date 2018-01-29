package org.funktionale.playground.bindings

import org.funktionale.either.Disjunction
import org.funktionale.either.flatMap
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn


fun <L, T> Disjunction.Companion.binding(f: suspend EitherContinuation<L, T>.() -> Disjunction<L, T>): Disjunction<L, T> {
    val continuation = EitherContinuation<L, T>()
    f.startCoroutine(continuation, continuation)
    return continuation.value
}

@RestrictsSuspension
class EitherContinuation<L, T>
internal constructor(override val context: CoroutineContext = EmptyCoroutineContext)
    : Continuation<Disjunction<L, T>> {
    lateinit var value: Disjunction<L, T>
    override fun resume(value: Disjunction<L, T>) {
        this.value = value
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    suspend fun <X> Disjunction<L, X>.bind(): X = suspendCoroutineOrReturn { c ->
        value = this.flatMap { x ->
            c.resume(x)
            value
        }
        COROUTINE_SUSPENDED
    }

    fun yields(value: T) = Disjunction.right(value)
}