package ru.alexmww.reactivewebflow

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import kotlin.random.Random

@SpringBootApplication
class ReactivewebflowApplication

fun main(args: Array<String>) {
    runApplication<ReactivewebflowApplication>(*args)
}

@Configuration
class ApiConfiguration {
    @Bean
    fun commentClient(): CommentClient {
        val client = WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com/comments")
                .build()
        return CommentClient(client)
    }

    @Bean
    fun postClient(): PostClient {
        val client = WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com/posts")
                .build()
        return PostClient(client)
    }
}

@Configuration
class RouterConfiguration(private val handler: Handler) {

    @Bean
    fun route() = coRouter {
        GET("/") { ok().bodyValueAndAwait("Hello from hardcore Kotlin web shit!") }
        "/api".nest {
            accept(TEXT_EVENT_STREAM).nest {
                GET("/data/{amount}", handler::handleRandomStream)
            }
            accept(APPLICATION_JSON).nest {
                GET("/data", handler::handleRandom)
            }
        }
    }
}

@Component
class Handler(private val client: Client) {
    suspend fun handleRandom(serverRequest: ServerRequest) = ok().bodyValueAndAwait(client.getRandomData())


    suspend fun handleRandomStream(serverRequest: ServerRequest) =
            ok().sse().bodyAndAwait(client.getRandomDataStream(serverRequest.pathVariable("amount").toInt()))
}

@Component
class Client(private val commentClient: CommentClient, private val postClient: PostClient) {

    fun getRandomDataStream(amount: Int) = flow {
        for (i in 1..amount) {
            println("invoked getData() $i times")
            emit(getRandomData())
        }
    }

    suspend fun getRandomData() = coroutineScope {
        val comment = async {
            println("getting comment")
            commentClient.getComment(Random.nextInt(500))
        }
        val post = async {
            println("getting post")
            postClient.getPost(Random.nextInt(100))
        }

        ResponseFromTwoApis(comment.await().email, post.await().title)
    }
}

data class Comment(val email: String)
class CommentClient(private val webClient: WebClient) {
    suspend fun getComment(commentId: Int): Comment {
        return webClient.get()
                .uri("/${commentId}")
                .accept(APPLICATION_JSON)
                .awaitExchange()
                .awaitBody()
    }

}

data class Post(val title: String)
class PostClient(private val webClient: WebClient) {

    suspend fun getPost(postId: Int): Post {
        return webClient.get()
                .uri("/${postId}")
                .accept(APPLICATION_JSON)
                .awaitExchange()
                .awaitBody()
    }
}

data class ResponseFromTwoApis(val firsApi: String, val secondApi: String)