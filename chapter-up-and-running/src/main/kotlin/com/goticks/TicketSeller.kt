package com.goticks

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import java.util.*

class TicketSeller(context: ActorContext<Command>, private val event: String) :
    AbstractBehavior<TicketSeller.Companion.Command>(context) {
    private var tickets: List<Ticket> = listOf()

    override fun onMessage(msg: Command?): Behavior<Command> =
        when (msg) {
            is Add -> add(msg)
            is Buy -> buy(msg)
            is Cancel -> cancel(msg)
            is GetEvent -> getEvent(msg)
            null -> Behaviors.same()
        }

    private fun add(add: Add): Behavior<Command> {
        val newTickets = add.tickets

        tickets = tickets + newTickets

        return Behaviors.same()
    }

    private fun buy(buy: Buy): Behavior<Command> {
        val nrOfTickets = buy.tickets
        val replyTo = buy.replyTo

        val entries = tickets.take(nrOfTickets)
        if (entries.size >= nrOfTickets) {
            replyTo.tell(Tickets(event, entries))
            tickets = tickets.drop(nrOfTickets)
        } else {
            replyTo.tell(Tickets(event))
        }

        return Behaviors.same()
    }

    private fun getEvent(getEvent: GetEvent): Behavior<Command> {
        val replyTo = getEvent.replyTo

        replyTo.tell(Optional.of(BoxOffice.Companion.Event(event, tickets.size)))

        return Behaviors.same()
    }

    private fun cancel(cancel: Cancel): Behavior<Command> {
        val replyTo = cancel.replyTo

        replyTo.tell(Optional.of(BoxOffice.Companion.Event(event, tickets.size)))

        return Behaviors.stopped() // poison pill
    }

    companion object {
        fun create(eventName: String): Behavior<Command> = Behaviors.setup { TicketSeller(it, eventName) }

        sealed interface Command
        data class Add(val tickets: List<Ticket>) : Command
        data class Buy(val tickets: Int, val replyTo: ActorRef<Tickets>) : Command
        data class GetEvent(val replyTo: ActorRef<Optional<BoxOffice.Companion.Event>>) : Command
        data class Cancel(val replyTo: ActorRef<Optional<BoxOffice.Companion.Event>>) : Command

        sealed interface Response
        data class Ticket(val id: Int)
        data class Tickets(val event: String, val entries: List<Ticket> = listOf()) : Response
    }
}