package aia.persistence

import aia.persistence.serialization.JacksonSerializable
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior

class Basket(persistentId: String) : EventSourcedBehavior<Basket.Command, Basket.Event, Basket.State>(PersistenceId.ofUniqueId(persistentId)) {
    sealed interface Command : Shopper.Command
    data class Add(val item: Item, override val shopperId: Long) : Command
    data class RemoveItem(val productId: String, override val shopperId: Long) : Command
    data class UpdateItem(val productId: String, val number: Int, override val shopperId: Long) : Command
    data class Clear(val shopperId: Long)
    data class Replace(val items: Items, override val shopperId: Long, ) : Command
    data class GetItems(override val shopperId: Long) : Command

    data class CountRecoveredEvents(override val shopperId: Long) : Command
    data class RecoveredEventsCount(val count: Long)

    sealed interface Event : JacksonSerializable
    data class Added(val item: Item) : Event
    data class ItemRemoved(val productId: String) : Event
    data class ItemUpdated(val productId: String, val number: Int) : Event
    data class Replaced(val items: Items) : Event
    data class Cleared(val clearedItems: Items) : Event

    data class Snapshot(val items: Items)

    data class State(val items: Items, val nrEventsRecovered: Int)

    override fun emptyState(): State {
        return State(Items(listOf()), 0)
    }

    override fun commandHandler(): CommandHandler<Command, Event, State> =
        newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Add::class.java) { _, command ->
                Effect().persist(Added(command.item))
            }
            .build()

    override fun eventHandler(): EventHandler<State, Event> =
        newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Added::class.java) { state, event ->
                state.copy(items = state.items.add(event.item))
            }
            .build()



}