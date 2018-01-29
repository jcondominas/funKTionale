package org.funktionale.playground.jordi

import kotlinx.coroutines.experimental.*
import org.funktionale.option.Option
import org.funktionale.playground.bindings.binding
import org.funktionale.tries.Try
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

fun main(argv: Array<String>) {
//    val try1 = Try<String> { "1" }
//    val try2 = Try { "2" }
//    val try3 = Try { 3 }
//    val option1 = 100.toOption()
//
//    val result = Try.binding<Int> {
//        println("start bindings")
//        val value1 = try1.bind()
//        println("bind1")
//        val value2 = try2.bind()
//        println("bind2")
//        val value3 = try3.bind()
//        println("bind3")
//        val valueOption = option1.toTry(Exception()).bind()
//        yields(value1.toInt() + value2.toInt() + value3 + valueOption)
//    }
//    result.onSuccess { print(it) }.onFailure { print("Error $it") }
//
//
//    val either1 = Either.right("1")
//    val either2 = Either.right("2")
//    val either3 = Either.right(3)
//    val either4 = Either.right(false)
//    val eitherResult = Disjunction.binding<Throwable, Int> {
//        val e1 = either1.toDisjunction().bind()
//        val e2 = either2.toDisjunction().bind()
//        val e3 = either3.toDisjunction().bind()
//        val e4 = either4.toDisjunction().bind()
//        yields(e1.toInt() + e2.toInt() + e3 + if (e4) 100 else -100)
//    }.toEither()
//    eitherResult.fold({ println("Error $it") }, { println("$it") })

    val result = userUseCase().flatMap { user ->
        favoriteUseCase().flatMap { favorite ->
            locationUseCase().flatMap { location ->
                "user is $user and is favorite: $favorite".toTry()
            }
        }
    }
    val job = launch {
        val userResult = async(CommonPool) {
            Try.binding<String> {
                val user = userUseCase().bind()
                val favorite = favoriteUseCase().bind()
                val location = locationUseCase(user).bind()
                yields("name: $user, is favorite: $favorite, and location $location")
            }
        }.await()

        userResult
                .onSuccess { println(it) }
                .onFailure { println("error: $it") }
    }
    runBlocking { job.join() }
}

fun <T> Option<T>.toTry(errorIfEmpty: Exception): Try<T> = this.fold({ Try.Failure(errorIfEmpty) }, { Try { it } })
fun <T> T.toTry() = Try.Success(this)

fun userUseCase(): Try<String> = runBlocking {
    "Jordi".toTry()
}

fun favoriteUseCase(): Try<Boolean> = runBlocking { true.toTry() }

fun locationUseCase(user: String): Try<Pair<Double, Double>> = Repository().getLocation(user)

class Repository {
    fun getLocation(user: String): Try<Pair<Double, Double>> = runBlocking {
        suspendCoroutine<Try<Pair<Double, Double>>> { c ->
            Api().getLocation {
                c.resume(it.toTry())
            }
        }
    }
}

class Api {
    fun getLocation(callback: (Pair<Double, Double>) -> Unit) {
        launch(newFixedThreadPoolContext(4, "")) {
            val result = async {
                delay(1, TimeUnit.SECONDS)
                Pair(1.0, 1.0)
//                throw Exception()
            }.await()
            callback(result)
        }
    }
}
