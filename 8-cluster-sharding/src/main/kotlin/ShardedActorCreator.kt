import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding
import java.time.Duration
import java.util.*

class ShardedActorCreator private constructor(context: ActorContext<Event>, val sharding: ClusterSharding) :
    AbstractBehavior<ShardedActorCreator.Event>(context) {
    interface Event : CborSerializable

    class CreateShardedActor : Event

    companion object {
        fun create(system: ActorSystem<*>): Behavior<Event> {
            return Behaviors.setup {
                ShardedActorCreator(
                    context = it,
                    sharding = ClusterSharding.get(system)
                )
            }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(CreateShardedActor::class.java) { onCreateShardedActor(it) }
            .build()
    }

    private fun onCreateShardedActor(event: CreateShardedActor): Behavior<Event> {
        val shardKey = Random().nextInt(1000).toString()
        context.log.info("[${context.system.address()}] Creating sharded actor with shard key: ${shardKey}")
        val ref = sharding.entityRefFor(ShardedActor.typeKey, "sharded-actor-${shardKey}")
        ref.tell(ShardedActor.Initialize())

        context.scheduleOnce(
            Duration.ofSeconds(1),
            context.self,
            CreateShardedActor()
        )
        return this
    }
}
