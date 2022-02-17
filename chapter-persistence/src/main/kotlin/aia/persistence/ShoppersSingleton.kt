package aia.persistence

import aia.persistence.serialization.JacksonSerializable
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.RecipientRef
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.SingletonActor
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior

class ShoppersSingleton(context: ActorContext<Shopper.Command>) : AbstractBehavior<Shopper.Command>(context) {
    companion object {
        fun create(): Behavior<Shopper.Command> {
            return Behaviors.setup { ShoppersSingleton(it) }
        }
        fun name() = "shoppers-singleton"
    }

    private val singletonManager = ClusterSingleton.get(context.system)
    private val shoppers = singletonManager.init(SingletonActor.of(Shoppers.create(), Shoppers.name()))

    override fun createReceive(): Receive<Shopper.Command> =
        newReceiveBuilder()
            .onMessage(Shopper.Command::class.java) {
                shoppers.tell(it)
                this
            }
            .build()
}

class Shoppers(override val context: akka.actor.typed.scaladsl.ActorContext<Shopper.Command>) : EventSourcedBehavior<Shopper.Command, Shoppers.Event, Shoppers.State>(PersistenceId.ofUniqueId("shoppers")), ShopperLookup {
    companion object {
        fun create(): Behavior<Shopper.Command> = akka.actor.typed.scaladsl.Behaviors.setup { Shoppers(it) }
        fun name() = "shoppers"
    }

    sealed interface Event : JacksonSerializable
    data class ShopperCreated(val shopperId: Long) : Event

    object State

    override var shopperNameToShopper: Map<String, RecipientRef<Shopper.Command>> = mapOf()

    override fun emptyState(): State {
        return State
    }

    override fun createShopper(shopperId: Long): RecipientRef<Shopper.Command> {
        val shopper = super.createShopper(shopperId)

        Effect().persist(ShopperCreated(shopperId))

        return shopper
    }

    override fun commandHandler(): CommandHandler<Shopper.Command, Event, State> =
        newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Shopper.Command::class.java) { _, command ->
                val shopperName = Shopper.name(command.shopperId)
                if (shopperNameToShopper.containsKey(shopperName)) {
                    Effect().none().thenRun { forwardToShopper(command) }
                } else {
                    Effect().persist(ShopperCreated(command.shopperId)).thenRun {
                        forwardToShopper(command)
                    }
                }
            }
            .build()

    override fun eventHandler(): EventHandler<State, Event> =
        newEventHandlerBuilder()
            .forAnyState()
            .onEvent(ShopperCreated::class.java) { state, (shopperId) ->
                val name = Shopper.name(shopperId)
                shopperNameToShopper = shopperNameToShopper + (name to createShopper(shopperId))
                state
            }
            .build()
}