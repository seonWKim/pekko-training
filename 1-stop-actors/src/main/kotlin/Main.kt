import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive

private val log = mu.KotlinLogging.logger { }

fun main(args: Array<String>) {
    ActorSystem.create(SelfStoppingActor.create(), "SelfStoppingActor_0").let { system ->
        system.tell(SelfStoppingActor.CreateChildEvent())
        system.tell(SelfStoppingActor.CloseChildEvent())
        system.tell(SelfStoppingActor.StopEvent())
    }
}

class SelfStoppingActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<SelfStoppingActor.Event>(context) {

    sealed interface Event

    class CreateChildEvent : Event

    class CloseChildEvent : Event

    class StopEvent : Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { SelfStoppingActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(CreateChildEvent::class.java) { onCreateChildEvent() }
            .onMessage(CloseChildEvent::class.java) { onCloseChildEvent() }
            .onMessage(StopEvent::class.java) { onStopEvent() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onCreateChildEvent(): Behavior<Event> {
        context.spawn(PassivelyStoppedActor.create(), "PassivelyStoppedActor")
        return this
    }

    private fun onCloseChildEvent(): Behavior<Event> {
        context.getChild("PassivelyStoppedActor").get().unsafeUpcast<Event>().tell(StopEvent())
        return this
    }

    private fun onStopEvent(): Behavior<Event> {
        log.info("Stopping SelfStoppingActor")
        return Behaviors.stopped()
    }

    private fun onPostStop(): Behavior<Event> {
        log.info("SelfStoppingActor stopped")
        return this
    }
}

class PassivelyStoppedActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<PassivelyStoppedActor.Event>(context) {

    sealed interface Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { PassivelyStoppedActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            // No messages are processed after PostStop signal 
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()
    }

    private fun onPostStop(): Behavior<Event> {
        log.info("PassivelyStoppedActor stopped")
        return this
    }
}
