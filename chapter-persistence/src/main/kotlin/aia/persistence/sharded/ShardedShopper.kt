package aia.persistence.sharded

import aia.persistence.Shopper
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.cluster.typed.Cluster
import akka.util.Timeout

class ShardedShopper(context: ActorContext<Command>, val parent: ActorRef<ClusterSharding.ShardCommand>) : Shopper(context.asScala()) {
    companion object {
        val typeKey = EntityTypeKey.create(Command::class.java, "Shopper")
        fun initSharding(system: ActorSystem<*>) {
            ClusterSharding.get(system).init(Entity.of(typeKey) {
                create(it.shard)
            }.withStopMessage(StopShopping(0L)))
        }

        fun create(parent: ActorRef<ClusterSharding.ShardCommand>): Behavior<Command> =
            Behaviors.setup {
                val passivateTimeout = it.system.settings().config().getDuration("passivate-timeout")
                it.setReceiveTimeout(passivateTimeout, Timeout(0L))
                ShardedShopper(it, parent)
            }

        fun name(shopperId: Long) = shopperId.toString()
    }

    data class Timeout(override val shopperId: Long) : Command
    data class StopShopping(override val shopperId: Long) : Command

    override fun unhandled(msg: Command): Behavior<Command> =
        when(msg) {
            is Timeout -> {
                parent.tell(ClusterSharding.Passivate(context().self()))
                this
            }
            is StopShopping -> {
                Behaviors.stopped()
            }
            else -> this
        }
}