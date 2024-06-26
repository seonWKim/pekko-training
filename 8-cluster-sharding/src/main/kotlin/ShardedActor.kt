import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.actor.typed.javadsl.Receive
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey

class ShardedActor(context: ActorContext<Event>, val shardKey: String) :
    AbstractBehavior<ShardedActor.Event>(context) {
    interface Event : CborSerializable

    class Initialize : Event
    companion object {
        // visit http://localhost:{managementPort}/cluster/shards/ShardActor to see the shard information 
        val typeKey: EntityTypeKey<Event> = EntityTypeKey.create(Event::class.java, "ShardedActor")
        fun initSharding(system: ActorSystem<*>) {
            ClusterSharding.get(system).init(
                Entity.of(typeKey) { entityContext -> create(entityContext.entityId) }
            )
        }

        private fun create(shardKey: String): Behavior<Event> {
            return Behaviors.setup { ShardedActor(context = it, shardKey = shardKey) }
        }
    }

    override fun createReceive(): Receive<Event> {
        return newReceiveBuilder()
            .onMessage(Initialize::class.java) { onInitialize(it) }
            .build()
    }

    private var initialized = false
    private fun onInitialize(event: Initialize): Behavior<Event> {
        if (initialized) {
            context.log.info("[${context.system.address()}] Already exists: $shardKey")
        } else {
            initialized = true
            context.log.info("[${context.system.address()}] Initialized: $shardKey")
        }
        return this
    }
}
