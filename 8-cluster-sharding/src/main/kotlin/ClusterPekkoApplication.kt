import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.javadsl.Behaviors
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding
import org.apache.pekko.management.javadsl.PekkoManagement

class ClusterPekkoApplication {
    companion object {
        private val DEFAULT_SERVER_INDEX = listOf(1, 2, 3)

        @JvmStatic
        fun main(args: Array<String>) {
            val ports = if (args.isEmpty()) {
                DEFAULT_SERVER_INDEX.map { 10000 + it }
            } else {
                runCatching {
                    args.map { it.toInt() }
                }.getOrElse {
                    DEFAULT_SERVER_INDEX.map { 10000 + it }
                }
            }

            ports.forEach { startup(it) }
        }

        private fun startup(port: Int) {
            val overrides = mapOf(
                "pekko.remote.artery.canonical.port" to port,   // server port
                "pekko.management.http.port" to (port + 10000),  // management port
                "pekko.cluster.jmx.multi-mbeans-in-same-jvm" to "on" // When running multiple nodes in the same JVM, this setting must be enabled
            )
            val config = ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load())

            // The name of the ActorSystem should match the value defined in "pekko.cluster.seed-nodes"
            val system = ActorSystem.create(initialize(), "ClusterPekkoApplication", config)
            val sharding = ClusterSharding.get(system)

            // Enable management
            // Endpoints: https://pekko.apache.org/docs/pekko-management/current/cluster-http-management.html
            PekkoManagement.get(system).start()

            system.log().info("Application started on port $port")
        }

        private fun initialize(): Behavior<Void> {
            return Behaviors.setup {
                it.spawn(ClusterListener.create(), "ClusterListener")
                Behaviors.empty()
            }
        }
    }
}
