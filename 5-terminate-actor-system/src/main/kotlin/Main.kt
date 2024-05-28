import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.PreRestart
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

fun main(args: Array<String>) {
    val system = ActorSystem.create(FiniteActor.create(), "FiniteActor")
    system.terminate()
}

class FiniteActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<FiniteActor.Event>(context) {

    interface Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { FiniteActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onSignal(PreRestart::class.java) { onPreStart() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onPreStart(): Behavior<Event> {
        context.log.info("FiniteActor started")
        return Behaviors.stopped()
    }

    private fun onPostStop(): Behavior<Event> {
        context.log.info("FiniteActor stopped")
        return this
    }
}
