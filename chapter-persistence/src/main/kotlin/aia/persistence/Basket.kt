package aia.persistence

import aia.persistence.serialization.JacksonSerializable
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior
import java.util.*

class Basket(context: ActorContext<Command>) :
    EventSourcedBehavior<Basket.Command, Basket.Event, Basket.State>(
        PersistenceId.ofUniqueId(
            context.self.path().name()
        )
    ) {
    companion object {
        fun create(): Behavior<Command> = Behaviors.setup { Basket(it) }
        fun name(shopperId: Long) = "basket_$shopperId"
    }

    sealed interface Command : Shopper.Command
    data class Add(val item: Item, override val shopperId: Long) : Command
    data class RemoveItem(
        val productId: String,
        override val shopperId: Long,
        val replyTo: ActorRef<Optional<ItemRemoved>>
    ) : Command

    data class UpdateItem(
        val productId: String,
        val number: Int,
        override val shopperId: Long,
        val replyTo: ActorRef<Optional<ItemUpdated>>
    ) : Command

    data class Clear(override val shopperId: Long) : Command
    data class Replace(val items: Items, override val shopperId: Long) : Command
    data class GetItems(override val shopperId: Long, val replyTo: ActorRef<Shopper.Command>) : Command

    data class CountRecoveredEvents(override val shopperId: Long, val replyTo: ActorRef<RecoveredEventsCount>) : Command
    data class RecoveredEventsCount(val count: Long)

    sealed interface Event : JacksonSerializable
    data class Added(val item: Item) : Event
    data class ItemRemoved(val productId: String) : Event
    data class ItemUpdated(val productId: String, val number: Int) : Event
    data class Replaced(val items: Items) : Event
    data class Cleared(val clearedItems: Items) : Event

    data class State(val items: Items, val nrEventsRecovered: Long) : JacksonSerializable {
        fun add(item: Item): State = copy(items = items.add(item))
        fun removeItem(id: String): State = copy(items = items.removeItem(id))
        fun updateItem(id: String, number: Int): State = copy(items = items.updateItem(id, number))
        fun replace(newItems: Items): State = copy(items = newItems)
        fun clear() = copy(items = items.clear())
    }

    override fun emptyState(): State {
        return State(Items(listOf()), 0)
    }

    override fun shouldSnapshot(state: State, event: Event, sequenceNr: Long): Boolean {
        return event is Cleared
    }

    override fun commandHandler(): CommandHandler<Command, Event, State> =
        newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Add::class.java) { _, (item) ->
                Effect().persist(Added(item))
            }
            .onCommand(RemoveItem::class.java) { (items), (id, _, replyTo) ->
                if (items.containsProduct(id)) {
                    val itemRemovedEvent = ItemRemoved(id)
                    Effect().persist(itemRemovedEvent).thenReply(replyTo) { Optional.of(itemRemovedEvent) }
                } else {
                    Effect().none().thenReply(replyTo) { Optional.empty() }
                }
            }
            .onCommand(UpdateItem::class.java) { (items), (id, number, _, replyTo) ->
                if (items.containsProduct(id)) {
                    val itemUpdateEvent = ItemUpdated(id, number)
                    Effect().persist(itemUpdateEvent).thenReply(replyTo) { Optional.of(itemUpdateEvent) }
                } else {
                    Effect().none().thenReply(replyTo) { Optional.empty() }
                }
            }
            .onCommand(Replace::class.java) { (items), _ ->
                Effect().persist(Replaced(items))
            }
            .onCommand(Clear::class.java) { (items), _ ->
                Effect().persist(Cleared(items))
            }
            .onCommand(GetItems::class.java) { (items), (shopperId, replyTo) ->
                Effect().none().thenReply(replyTo) { Shopper.BasketGetItemsResponse(items, shopperId) }
            }
            .onCommand(CountRecoveredEvents::class.java) { (_, nrEventsRecovered), (_, replyTo) ->
                Effect().none().thenReply(replyTo) { RecoveredEventsCount(nrEventsRecovered) }
            }
            .build()

    override fun eventHandler(): EventHandler<State, Event> =
        newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Added::class.java) { state, (item) ->
                state.add(item)
            }
            .onEvent(ItemRemoved::class.java) { state, (id) ->
                state.removeItem(id)
            }
            .onEvent(ItemUpdated::class.java) { state, (id, number) ->
                state.updateItem(id, number)
            }
            .onEvent(Replaced::class.java) { state, (newItems) ->
                state.replace(newItems)
            }
            .onEvent(Cleared::class.java) { state, _ ->
                state.clear()
            }
            .build()
}