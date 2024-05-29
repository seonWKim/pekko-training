import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import java.time.Duration

class GlobalScheduler private constructor(context: ActorContext<Event>) :
    AbstractBehavior<GlobalScheduler.Event>(context) {

    interface Event : CborSerializable

    data class Schedule(val text: String, val delay: Duration) : Event
    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { GlobalScheduler(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(Schedule::class.java) { onSchedule(it) }
            .build()
    }

    // You will only see single node printing out the text
    private fun onSchedule(event: Schedule): Behavior<Event> {
        context.log.info("[${context.system.address()}] GlobalScheduler working. Printing out text: ${event.text}")
        context.scheduleOnce(event.delay, context.self, event)
        return this
    }
}
