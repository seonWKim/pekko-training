import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

fun main(args: Array<String>) {
    ActorSystem.create(PrintActor.create(), "PrintActor_0").tell(PrintActor.StartPrintCommand("Hello, World!"))
    ActorSystem.create(PrintActor.create(), "PrintActor_1").tell(PrintActor.StartPrintCommand("Hello, Kotlin!"))
    ActorSystem.create(PrintActor.create(), "PrintActor_2").tell(PrintActor.StartPrintCommand("Hello, Actor!"))
}

class PrintActor(context: ActorContext<Event>) :
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
        context.log.info("[${context.self}] ${event.text}")
        return this
    }
}
