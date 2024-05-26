import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

fun main(args: Array<String>) {

    val system = ActorSystem.create(PrintActor.create(), "PrintActor")
    system.tell(PrintActor.StartPrintCommand("Hello, World!"))
}

class PrintActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<PrintActor.Event>(context) {

    sealed interface Event

    data class StartPrintCommand(val text: String) : Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { PrintActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(StartPrintCommand::class.java, this::onStartPrintCommand)
            .build()
    }

    private fun onStartPrintCommand(event: StartPrintCommand): Behavior<Event> {
        println("[${context.self}] ${event.text}")
        return this
    }
}
