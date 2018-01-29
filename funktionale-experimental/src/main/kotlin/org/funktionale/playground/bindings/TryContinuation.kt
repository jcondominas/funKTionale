package org.funktionale.playground.bindings

import org.funktionale.tries.Try
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

fun <T> Try.Companion.binding(f: suspend TryContinuation<T>.() -> Try<T>): Try<T> {
    val continuation = TryContinuation<T>()
    f.startCoroutine(continuation, continuation)
    return continuation.value
}

@RestrictsSuspension
class TryContinuation<T>(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Try<T>> {
    lateinit var value: Try<T>
    override fun resume(value: Try<T>) {
        println("value: $value")
        this.value = value
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    suspend fun <X> Try<X>.bind(): X = suspendCoroutineOrReturn { c ->
        println("suspend coroutine or return")
        value = this.flatMap { x ->
            println("X is $x and before resume")
            c.resume(x)
            println("X is $x and after resume, and value is $value")
            value
        }
        COROUTINE_SUSPENDED
    }

    fun yields(value: T) = Try { value }
}