import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import java.time.Duration


fun main(args: Array<String>) {
    val system = ActorSystem.create(SupervisingActor.create(), "supervisingActor")

    // Default: stops the actor  
    system.tell(SupervisingActor.CreateChild(strategy = SupervisorStrategy.stop()))
    system.tell(SupervisingActor.FailChild())

    // Retry with limit
    val retryCount = 2
    system.tell(SupervisingActor.CreateChild(strategy = SupervisorStrategy.restart().withLimit(retryCount, Duration.ofSeconds(1))))
    repeat(retryCount) {
        system.tell(SupervisingActor.FailChild())
    }

    // Resume: store the state before exception is thrown
    system.tell(SupervisingActor.CreateChild(strategy = SupervisorStrategy.resume()))
    system.tell(SupervisingActor.IncreaseChildCounter())
    system.tell(SupervisingActor.IncreaseChildCounter())
    system.tell(SupervisingActor.FailChild())
    system.tell(SupervisingActor.IncreaseChildCounter())
}

class SupervisingActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<SupervisingActor.Event>(context) {

    sealed interface Event

    class CreateChild(val strategy: SupervisorStrategy) : Event

    class IncreaseChildCounter: Event

    class FailChild : Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { SupervisingActor(it) }
        }
    }

    private var child: ActorRef<ChildActor.Event>? = null

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(CreateChild::class.java) { onCreateChild(it) }
            .onMessage(IncreaseChildCounter::class.java) { onIncreaseChildCounter() }
            .onMessage(FailChild::class.java) { onFailChild() }
            .build()
    }

    private fun onCreateChild(event: CreateChild): Behavior<Event> {
        val strategy = event.strategy
        child = context.spawn(Behaviors.supervise(ChildActor.create()).onFailure(strategy), "child")
        return this
    }

    private fun onIncreaseChildCounter(): Behavior<Event> {
        child?.tell(ChildActor.Increase())
        return this
    }

    private fun onFailChild(): Behavior<Event> {
        child?.tell(ChildActor.Fail())
        return this
    }
}

class ChildActor private constructor(context: ActorContext<Event>) :
    AbstractBehavior<ChildActor.Event>(context) {

    sealed interface Event

    class Fail : Event

    class Increase: Event

    private var counter: Long = 0

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { ChildActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onSignal(PreRestart::class.java) { onPreRestart() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .onMessage(Fail::class.java) { onFail() }
            .onMessage(Increase::class.java) { onIncrease() }
            .build()
    }

    private fun onPreRestart(): Behavior<Event> {
        context.log.info("ChildActor will be restarted")
        return this
    }

    private fun onPostStop(): Behavior<Event> {
        context.log.info("ChildActor onPostStop")
        return this
    }

    private fun onFail(): Behavior<Event> {
        context.log.info("ChildActor failed")
        throw RuntimeException("ChildActor failed")
    }

    private fun onIncrease(): Behavior<Event> {
        counter++
        context.log.info("ChildActor counter: $counter")
        return this
    }
}
