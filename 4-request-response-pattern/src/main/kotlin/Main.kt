import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import java.util.*

fun main(args: Array<String>) {
    val system = ActorSystem.create(ClientActor.create(), "ClientActor")
    repeat(3) {
        system.tell(ClientActor.SendApiRequestToServerCommand("GET /api/v1/users/${it + 1}"))
    }
}

class ClientActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<ClientActor.Event>(context) {

    sealed interface Event

    data class SendApiRequestToServerCommand(val command: String) : Event

    data class ApiSuccessfullyHandledEvent(val requestId: Long) : Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { ClientActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(SendApiRequestToServerCommand::class.java) { onSendApiRequestToServerCommand(it) }
            .onMessage(ApiSuccessfullyHandledEvent::class.java) { onApiSuccessfullyHandledEvent(it) }
            .build()
    }

    private fun onSendApiRequestToServerCommand(command: SendApiRequestToServerCommand): Behavior<Event> {
        val server = context.getChild("ServerActor").let {
            if (!it.isPresent) {
                context.spawn(ServerActor.create(), "ServerActor")
            } else {
                it.get()
            }
        }.unsafeUpcast<ServerActor.Event>()
        
        server.tell(
            ServerActor.ReceiveRequestCommand(
                client = context.self,
                requestId = Random().nextLong(),
                command = command.command
            )
        )
        return this
    }

    private fun onApiSuccessfullyHandledEvent(event: ApiSuccessfullyHandledEvent): Behavior<Event> {
        context.log.info("API request successfully handled: $event")
        return this 
    }
}

class ServerActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<ServerActor.Event>(context) {

    sealed interface Event

    data class ReceiveRequestCommand(
        val client: ActorRef<ClientActor.Event>,
        val requestId: Long,
        val command: String
    ) : Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { ServerActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(ReceiveRequestCommand::class.java) { onReceiveRequestCommand(it) }
            .build()
    }

    private fun onReceiveRequestCommand(command: ReceiveRequestCommand): Behavior<Event> {
        context.log.info("Received request: ${command.command}")
        command.client.tell(ClientActor.ApiSuccessfullyHandledEvent(command.requestId))
        return this
    }
}
