import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import org.apache.pekko.cluster.ClusterEvent
import org.apache.pekko.cluster.typed.Cluster
import org.apache.pekko.cluster.typed.Subscribe

class ClusterListener : AbstractBehavior<ClusterListener.Event> {
    interface Event {}

    data class ReachabilityChange(val reachabilityEvent: ClusterEvent.ReachabilityEvent) : Event

    data class MemberChange(val memberEvent: ClusterEvent.MemberEvent) : Event

    companion object {
        fun create(): Behavior<Event> {
            return Behaviors.setup { ClusterListener(it) }
        }
    }

    private constructor(context: ActorContext<Event>) : super(context) {
        val cluster = Cluster.get(context.system)

        val memberEventAdapter =
            context.messageAdapter(ClusterEvent.MemberEvent::class.java) { MemberChange(it) }
        cluster.subscriptions().tell(Subscribe.create(memberEventAdapter, ClusterEvent.MemberEvent::class.java))

        val reachabilityAdaptor =
            context.messageAdapter(ClusterEvent.ReachabilityEvent::class.java) { ReachabilityChange(it) }
        cluster.subscriptions()
            .tell(Subscribe.create(reachabilityAdaptor, ClusterEvent.ReachabilityEvent::class.java))
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(ReachabilityChange::class.java, this::onReachabilityChange)
            .onMessage(MemberChange::class.java, this::onMemberChange)
            .build()
    }

    private fun onReachabilityChange(event: ReachabilityChange): Behavior<Event> {
        when (event.reachabilityEvent) {
            is ClusterEvent.UnreachableMember -> {
                println("[UnreachableMember] Member is unreachable: ${event.reachabilityEvent.member()}")
            }

            is ClusterEvent.ReachableMember -> {
                println("[ReachableMember] Member is reachable: ${event.reachabilityEvent.member()}")
            }

            else -> {}
        }

        return this
    }

    private fun onMemberChange(event: MemberChange): Behavior<Event> {
        when (event.memberEvent) {
            is ClusterEvent.MemberUp -> {
                println("[ClusterEvent.MemberUp] Member is up: ${event.memberEvent.member()}")
            }

            is ClusterEvent.MemberRemoved -> {
                println("[ClusterEvent.MemberRemoved] Member is removed: ${event.memberEvent.member()}")
            }

            else -> {}
        }

        return this
    }
}
