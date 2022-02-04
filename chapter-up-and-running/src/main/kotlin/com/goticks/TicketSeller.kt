package com.goticks

import akka.actor.AbstractActor
import akka.actor.PoisonPill
import akka.actor.Props

class TicketSeller(private val event: String) : AbstractActor() {
    private val tickets: MutableList<Ticket> = mutableListOf()

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(Add::class.java, this::add)
            .match(Buy::class.java, this::buy)
            .match(GetEvent::class.java, this::getEvent)
            .match(Cancel::class.java, this::cancel)
            .build()
    }

    private fun add(add: Add) {
        val newTickets = add.tickets

        tickets.addAll(newTickets)
    }

    private fun buy(buy: Buy) {
        val nrOfTickets = buy.tickets

        val entries = tickets.take(nrOfTickets)
        if (entries.size >= nrOfTickets) {
            sender.tell(Tickets(event, entries), self)
            tickets.drop(nrOfTickets)
        } else {
            sender.tell(Tickets(event), self)
        }
    }

    private fun getEvent(getEvent: GetEvent) {
        sender.tell(BoxOffice.Companion.Event(event, tickets.size), self)
    }

    private fun cancel(cancel: Cancel) {
        sender.tell(BoxOffice.Companion.Event(event, tickets.size), self)
        self.tell(PoisonPill.getInstance(), self)
    }

    companion object {
        fun props(event: String): Props {
            return Props.create(TicketSeller::class.java) { TicketSeller(event) }
        }

        data class Add(val tickets: List<Ticket>)
        data class Buy(val tickets: Int)
        data class Ticket(val id: Int)
        data class Tickets(val event: String, val entries: List<Ticket> = listOf())
        object GetEvent
        object Cancel
    }
}