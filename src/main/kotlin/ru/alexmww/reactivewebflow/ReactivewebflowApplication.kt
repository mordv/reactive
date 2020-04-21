package ru.alexmww.reactivewebflow

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import kotlin.random.Random

@SpringBootApplication
class ReactivewebflowApplication

fun main(args: Array<String>) {
    runApplication<ReactivewebflowApplication>(*args)
}

//@Configuration
//class Controller(private val client: Client) {
//
//    @Bean
//    fun route() = router {
//        GET("/") {ok().render("index")}
//        "/api".nest {
//            GET("/random", client::getData)
//        }
//    }
//}

@RestController
@RequestMapping("/api")
class Controller(private val client: Client) {
    @GetMapping("/random")
    suspend fun getRandom(): ResponseFromTwoApis {
        return client.getData();
    }

    @GetMapping("/stream/{amount}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@PathVariable amount: Int): Flow<ResponseFromTwoApis> {
        return client.get(amount)
    }
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


@Component
class Client(private val commentClient: CommentClient, private val postClient: PostClient) {

    fun get(amount: Int) = flow {
        for (i in 1..amount) {
//            delay(100)
            println("invoke getData() $i times")
            emit(getData())
        }
    }

    suspend fun getData() = coroutineScope {
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
                .accept(MediaType.APPLICATION_JSON)
                .awaitExchange()
                .awaitBody()
    }

}

data class Post(val title: String)
class PostClient(private val webClient: WebClient) {

    suspend fun getPost(postId: Int): Post {
        return webClient.get()
                .uri("/${postId}")
                .accept(MediaType.APPLICATION_JSON)
                .awaitExchange()
                .awaitBody()
    }
}

data class ResponseFromTwoApis(val firsApi: String, val secondApi: String)