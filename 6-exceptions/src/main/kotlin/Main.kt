import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.PreRestart
import org.apache.pekko.actor.typed.SupervisorStrategy
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import java.time.Duration

// Uncomment the strategy you want to demonstrate
fun main(args: Array<String>) {
      defaultStrategy()
     // restartStrategy()
     // resumeStrategy()
}

// Demonstrates the default supervisor strategy: stop the actor when it fails
fun defaultStrategy() {
    val system = ActorSystem.create(ConditionallyExceptionThrowingActor.create(), "ConditionallyExceptionThrowingActor")
    system.log().info("defaultStrategy started")
    // Send messages to the actor. The third message will cause the actor to fail and be stopped.
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 1))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 2))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = true, id = 3))

    // Any further messages will be sent to the dead letters.
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 4)) // to dead letter
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 5)) // to dead letter
    system.terminate()
    system.log().info("defaultStrategy ended")
}

// Demonstrates the restart supervisor strategy: restart the actor when it fails
fun restartStrategy() {
    val actor = Behaviors.supervise(ConditionallyExceptionThrowingActor.create())
        .onFailure(SupervisorStrategy.restart().withLimit(3, Duration.ofSeconds(1)))
    val system = ActorSystem.create(actor, "ConditionallyExceptionThrowingActor")
    system.log().info("restartStrategy started")
    // Send messages to the actor. The third message will cause the actor to fail and be restarted.
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 1))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 2))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = true, id = 3))

    // Number of total messages received will be reset after the actor is restarted
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 4))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 5))
    system.terminate()
    system.log().info("restartStrategy ended")
}

// Demonstrates the resume supervisor strategy: resume message processing when the actor fails, without restarting it
fun resumeStrategy() {
    val actor = Behaviors.supervise(ConditionallyExceptionThrowingActor.create())
        .onFailure(SupervisorStrategy.resume())
    val system = ActorSystem.create(actor, "ConditionallyExceptionThrowingActor")
    system.log().info("resumeStrategy started")
    // Send messages to the actor. The third message will cause the actor to fail, but it will resume processing the next message.
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 1))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 2))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = true, id = 3))

    // Preserves the state of the actor after the exception
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 4))
    system.tell(ConditionallyExceptionThrowingActor.ThrowExceptionCommand(shouldThrow = false, id = 5))
    system.terminate()
    system.log().info("resumeStrategy ended")
}

class ConditionallyExceptionThrowingActor private constructor(context: ActorContext<Event>):
    AbstractBehavior<ConditionallyExceptionThrowingActor.Event>(context) {
    private var exceptionCount = 0
    private var messagesReceived = 0

    sealed interface Event

    data class ThrowExceptionCommand(val shouldThrow: Boolean, val id: Long): Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { ConditionallyExceptionThrowingActor(it) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onSignal(PreRestart::class.java) { onPreRestart() }
            .onMessage(ThrowExceptionCommand::class.java, this::onThrowExceptionCommand)
            .build()
    }

    private fun onPreRestart(): Behavior<Event> {
        context.log.info("ConditionallyExceptionThrowingActor restarted")
        return this
    }

    private fun onThrowExceptionCommand(event: ThrowExceptionCommand): Behavior<Event> {
        messagesReceived++
        if (event.shouldThrow && exceptionCount < 1) {
            exceptionCount++
            throw RuntimeException("Irrecoverable error occurred")
        }

        context.log.info("No exception thrown. Id: ${event.id}. Total number of messages received: $messagesReceived")
        exceptionCount = 0
        return this
    }
}
