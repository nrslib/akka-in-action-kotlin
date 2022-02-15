package aia.persistence

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

class LocalShoppers(override val context: ActorContext<Shopper.Command>) : AbstractBehavior<Shopper.Command>(context),
    ShopperLookup {
    override var shopperNameToShopper: Map<String, ActorRef<Shopper.Command>> = mapOf()

    companion object {
        fun create(): Behavior<Shopper.Command> = Behaviors.setup { LocalShoppers(it) }
        val name = "local-shoppers"
    }

    override fun onMessage(msg: Shopper.Command): Behavior<Shopper.Command> {
        forwardToShopper(msg)
        return this
    }
}

interface ShopperLookup {
    val context: ActorContext<Shopper.Command>
    var shopperNameToShopper: Map<String, ActorRef<Shopper.Command>>

    fun forwardToShopper(cmd: Shopper.Command) {
        val shopperName = Shopper.name(cmd.shopperId)
        val shopper = shopperNameToShopper[shopperName] ?: createShopper(cmd.shopperId)
        shopper.tell(cmd)
    }

    fun createAndForward(cmd: Shopper.Command, shopperId: Long) =
        createShopper(shopperId).tell(cmd)

    fun createShopper(shopperId: Long): ActorRef<Shopper.Command> {
        val actorName = Shopper.name(shopperId)
        val actor = context.spawn(Shopper.create(), actorName, Props.empty())

        shopperNameToShopper = shopperNameToShopper + (actorName to actor)

        return actor
    }
}

