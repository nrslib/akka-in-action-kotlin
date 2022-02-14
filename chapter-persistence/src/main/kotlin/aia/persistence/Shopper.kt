package aia.persistence

import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import java.math.BigDecimal


class Shopper(context: ActorContext<Command>) : AbstractBehavior<Shopper.Command>(context) {
    companion object {
        val cash: BigDecimal = BigDecimal(40000)
        fun name(shopperId: Long) = shopperId.toString()
    }

    sealed interface Command {
        val shopperId: Long
    }
    data class PayBasket(override val shopperId: Long) : Command

    val shopperId = context.self().path().name().toLong()
    val wallet = context.spawn(Wallet.create(shopperId, cash), name(shopperId), Props.empty())

    override fun onMessage(msg: Command): Behavior<Command> =
        when(msg) {
            is Wallet.Command -> {
                wallet.tell(msg)
                this
            }
            is PayBasket -> {
                this
            }
            else -> {this}
        }
}