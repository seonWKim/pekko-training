import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.http.javadsl.Http
import org.apache.pekko.http.javadsl.ServerBinding
import org.apache.pekko.http.javadsl.model.ws.Message
import org.apache.pekko.http.javadsl.model.ws.TextMessage
import org.apache.pekko.http.javadsl.server.AllDirectives
import org.apache.pekko.http.javadsl.server.Route
import org.apache.pekko.japi.JavaPartialFunction.noMatch
import org.apache.pekko.stream.javadsl.Flow
import org.apache.pekko.stream.javadsl.Source

class HttpServerWebSocketExample : AllDirectives() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val system = ActorSystem.create(Behaviors.empty<Void>(), "routes")
            val http = Http.get(system)
            val app = WebSocketRouter(system)
            val binding = http.newServerAt("localhost", 8080).bind(app.createRoute())

            println("Server online at http://localhost:8080/\nPress RETURN to stop...")
            readLine()

            binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept { system.terminate() }
        }
    }
}


class WebSocketRouter(val system: ActorSystem<*>) : AllDirectives() {
    fun createRoute(): Route {
        return path("greeter") { handleWebSocketMessages(greeter()) }
    }

    private fun greeter(): Flow<Message, Message, NotUsed> {
        return Flow.create<Message>().map { msg ->
            if (msg.isText) {
                handleTextMessage(msg.asTextMessage())
            } else {
                throw noMatch()
            }
        }
    }

    private fun handleTextMessage(msg: TextMessage): TextMessage {
        return if (msg.isStrict) {
            system.log().info("Received: ${msg.getStrictText()}")
            TextMessage.create("Hello ${msg.getStrictText()}")
        } else {
            system.log().info("Received: ${msg.getStreamedText()}")
            TextMessage.create(Source.single("Hello ").concat(msg.getStreamedText()))
        }
    }
}
