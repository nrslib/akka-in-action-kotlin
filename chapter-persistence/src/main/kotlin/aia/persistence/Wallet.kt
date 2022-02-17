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
import java.math.BigDecimal

class Wallet(private val shopperId: Long, private val cash: BigDecimal, context: ActorContext<Command>) :
    EventSourcedBehavior<Wallet.Command, Wallet.Event, Wallet.State>(
        PersistenceId.ofUniqueId(
            context.self.path().name()
        )
    ) {
    companion object {
        fun create(shopperId: Long, cash: BigDecimal): Behavior<Command> = Behaviors.setup {
            Wallet(shopperId, cash, it)
        }

        fun name(shopperId: Long) = "wallet_$shopperId"
    }

    sealed interface Command : Shopper.Command
    data class Pay(val items: List<Item>, override val shopperId: Long, val replyTo: ActorRef<Shopper.Command>) :
        Command

    data class Check(override val shopperId: Long, val replyTo: ActorRef<Cash>) : Command
    data class SpentHowMuch(override val shopperId: Long, val replyTo: ActorRef<AmountSpent>) : Command

    data class AmountSpent(val amount: BigDecimal)
    data class Cash(val left: BigDecimal)

    sealed interface Event : JacksonSerializable
    data class Paid(val list: List<Item>, val shopperId: Long) : Event

    data class State(val list: List<Item>, val amountSpent: BigDecimal) : JacksonSerializable

    override fun emptyState(): State {
        return State(listOf(), BigDecimal.ZERO)
    }

    override fun commandHandler(): CommandHandler<Command, Event, State> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Pay::class.java) { state, command ->
                val items = command.items
                val totalSpent = addSpending(state, items)
                if (cash - totalSpent > BigDecimal.ZERO) {
                    Effect().persist(Paid(items, shopperId))
                        .thenReply(command.replyTo) { Shopper.WalletPayResponse(totalSpent, shopperId) }
                } else {
                    // context.system.eventStream.publish(NotEnoughCash(cash - amountSpent))
                    Effect().none()
                }
            }
            .onCommand(Check::class.java) { state, command ->
                Effect().none().thenReply(command.replyTo) { Cash(cash - state.amountSpent) }
            }
            .onCommand(SpentHowMuch::class.java) { state, command ->
                Effect().none().thenReply(command.replyTo) { AmountSpent(state.amountSpent) }
            }
            .build()

    }

    override fun eventHandler(): EventHandler<State, Event> =
        newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Paid::class.java) { state, paid ->
                state.copy(amountSpent = addSpending(state, paid.list))
            }
            .build()

    private fun addSpending(state: State, items: List<Item>): BigDecimal =
        state.amountSpent + items.fold(BigDecimal.ZERO) { total, item ->
            total + item.unitPrice.multiply(item.number.toBigDecimal())
        }
}