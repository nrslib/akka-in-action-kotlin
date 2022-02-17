package aia.persistence.sharded

import aia.persistence.Shopper
import aia.persistence.ShopperLookup
import akka.actor.typed.Behavior
import akka.actor.typed.RecipientRef
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.javadsl.ClusterSharding

class ShardedShoppers(override val context: ActorContext<Shopper.Command>, private val sharding: ClusterSharding) : AbstractBehavior<Shopper.Command>(context), ShopperLookup {
    companion object {
        fun create(): Behavior<Shopper.Command> = Behaviors.setup {
            ShardedShopper.initSharding(it.system())

            ShardedShoppers(it, ClusterSharding.get(it.system()))
        }
        fun name() = "sharded-shoppers"
    }

    override var shopperNameToShopper: Map<String, RecipientRef<Shopper.Command>> = mapOf()

    override fun onMessage(msg: Shopper.Command): Behavior<Shopper.Command> {
        forwardToShopper(msg)
        return this
    }

    override fun createShopper(shopperId: Long): RecipientRef<Shopper.Command> {
        return sharding.entityRefFor(ShardedShopper.typeKey, shopperId.toString())
    }
}