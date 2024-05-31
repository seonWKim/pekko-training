import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.http.javadsl.Http
import org.apache.pekko.http.javadsl.ServerBinding
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson
import org.apache.pekko.http.javadsl.model.StatusCodes
import org.apache.pekko.http.javadsl.server.AllDirectives
import org.apache.pekko.http.javadsl.server.PathMatchers.longSegment
import org.apache.pekko.http.javadsl.server.Route
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class HttpServerBasicExample : AllDirectives() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val system = ActorSystem.create(Behaviors.empty<Void>(), "routes")
            val http = Http.get(system)
            val app = HttpServerBasicExample()
            val binding = http.newServerAt("localhost", 8080).bind(app.createRoute())

            println("Server online at http://localhost:8080/\nPress RETURN to stop...")
            readLine()

            binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept { system.terminate() }
        }
    }

    private fun fetchItem(itemId: Long): CompletionStage<Optional<Item>> {
        return CompletableFuture.completedFuture(Optional.of(Item("foo", itemId)))
    }

    private fun saveOrder(order: Order): CompletionStage<Done> {
        return CompletableFuture.completedFuture(Done.getInstance())
    }

    private fun createRoute(): Route {
        return concat(
            get {
                pathPrefix("item") {
                    path(longSegment()) { id ->
                        val futureMaybeItem = fetchItem(id)
                        onSuccess(futureMaybeItem) { maybeItem ->
                            maybeItem.map { item -> completeOK(item, Jackson.marshaller()) }
                                .orElseGet { complete(StatusCodes.NOT_FOUND, "Not Found") }
                        }
                    }
                }
            },
            post {
                path("create-order") {
                    entity(Jackson.unmarshaller(Order::class.java)) { order ->
                        val futureSaved = saveOrder(order)
                        onSuccess(futureSaved) { complete("order created") }
                    }
                }
            }
        )
    }

    data class Item(@JsonProperty("name") val name: String, @JsonProperty("id") val id: Long)

    data class Order(@JsonProperty("items") val items: List<Item>)
}
