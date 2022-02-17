package aia.persistence

import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import java.math.BigDecimal


open class Shopper(context: ActorContext<Command>) : AbstractBehavior<Shopper.Command>(context) {
    companion object {
        fun create(): Behavior<Command> = Behaviors.setup { Shopper(it) }
        val cash: BigDecimal = BigDecimal(40000)
        fun name(shopperId: Long) = shopperId.toString()
    }

    interface Command {
        val shopperId: Long
    }

    data class PayBasket(override val shopperId: Long) : Command
    data class BasketGetItemsResponse(val items: Items, override val shopperId: Long) : Command
    data class WalletPayResponse(val totalSpent: BigDecimal, override val shopperId: Long) : Command

    private val shopperId = context.self().path().name().toLong()
    private val basket = context.spawn(Basket.create(), Basket.name(shopperId), Props.empty())
    private val wallet = context.spawn(Wallet.create(shopperId, cash), Wallet.name(shopperId), Props.empty())

    override fun onMessage(msg: Command): Behavior<Command> =
        when (msg) {
            is Wallet.Command -> {
                wallet.tell(msg)
                this
            }
            is Basket.Command -> {
                basket.tell(msg)
                this
            }

            is PayBasket -> {
                basket.tell(Basket.GetItems(msg.shopperId, context().self()))
                this
            }
            is BasketGetItemsResponse -> {
                wallet.tell(Wallet.Pay(msg.items.list, msg.shopperId, context().self()))
                this
            }
            is WalletPayResponse -> {
                basket.tell(Basket.Clear(msg.shopperId))
                this
            }
            else -> unhandled(msg)
        }

    open fun unhandled(msg: Command): Behavior<Command> = this
}