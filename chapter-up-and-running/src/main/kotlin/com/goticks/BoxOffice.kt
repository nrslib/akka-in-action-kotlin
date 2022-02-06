package com.goticks

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.javadsl.AskPattern
import akka.actor.typed.scaladsl.*
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

class BoxOffice(context: ActorContext<Command>, private val timeout: Duration) : AbstractBehavior<BoxOffice.Companion.Command>(context) {
    val tsNameToTicketSeller = hashMapOf<String, ActorRef<TicketSeller.Companion.Command>>()

    private fun createTicketSeller(name: String): ActorRef<TicketSeller.Companion.Command> {
        val ticketSeller = context().spawn(TicketSeller.create(name), name, Props.empty())
        tsNameToTicketSeller[name] = ticketSeller
        return ticketSeller
    }

    override fun onMessage(msg: Command?): Behavior<Command> =
        when (msg) {
            is CreateEvent -> createEvent(msg)
            is GetTickets -> getTickets(msg)
            is GetEvent -> getEvent(msg)
            is GetEvents -> getEvents(msg)
            is CancelEvent -> cancelEvent(msg)
            is TicketSellerTerminated -> ticketSellerTerminated(msg)
            null -> Behaviors.same()
        }


    fun createEvent(event: CreateEvent): Behavior<Command> {
        val name = event.name
        val tickets = event.tickets
        val replyTo = event.replyTo

        fun create() {
            val eventTickets = createTicketSeller(name)

            val newTickets = (1..tickets).map { TicketSeller.Companion.Ticket(it) }.toList()
            eventTickets.tell(TicketSeller.Companion.Add(newTickets))
            replyTo.tell(EventCreated(Event(name, tickets)))
        }

        findTicketSellerThen(name, { replyTo.tell(EventExists) }, ::create)

        return Behaviors.same()
    }

    fun getTickets(getTickets: GetTickets): Behavior<Command> {
        val event = getTickets.event
        val tickets = getTickets.tickets
        val replyTo = getTickets.replyTo

        fun notFound() = replyTo.tell(TicketSeller.Companion.Tickets(event))
        fun buy(child: ActorRef<TicketSeller.Companion.Command>) =
            child.tell(TicketSeller.Companion.Buy(tickets, replyTo))

        findTicketSellerThen(event, ::buy, ::notFound)

        return Behaviors.same()
    }

    fun getEvent(getEvent: GetEvent): Behavior<Command>  {
        val event = getEvent.name
        val replyTo = getEvent.replyTo

        fun notFound() = replyTo.tell(Optional.empty())
        fun getEvent(child: ActorRef<TicketSeller.Companion.Command>) = child.tell(TicketSeller.Companion.GetEvent(replyTo))

        findTicketSellerThen(event, ::getEvent, ::notFound)

        return Behaviors.same()
    }

    fun getEvents(getEvents: GetEvents): Behavior<Command>  {
        val replyTo = getEvents.replyTo

        fun getEventFutures() = tsNameToTicketSeller.values
            .map {
                AskPattern.ask(context().self(), {replyTo: ActorRef<Optional<Event>> -> GetEvent(it.path().name(), replyTo) }, timeout, context().system().scheduler())
                    .toCompletableFuture()
            }

        val futures = getEventFutures()
        val futureEvents = CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply {
                val events = futures.stream()
                    .map { it.join() }
                    .filter { it.isPresent }
                    .map { it.get() }
                    .toList()
                Events(events)
            }

        futureEvents
            .thenApply { replyTo.tell(it) }

        return Behaviors.same()
    }

    fun cancelEvent(cancelEvent: CancelEvent): Behavior<Command>  {
        val event = cancelEvent.name
        val replyTo = cancelEvent.replyTo

        fun notFound() = replyTo.tell(Optional.empty())
        fun cancelEvent(child: ActorRef<TicketSeller.Companion.Command>) {
            child.tell(TicketSeller.Companion.Cancel(replyTo))
            context().watchWith(child, TicketSellerTerminated(event))
        }

        findTicketSellerThen(event, ::cancelEvent, ::notFound)

        return Behaviors.same()
    }

    fun ticketSellerTerminated(msg: TicketSellerTerminated): Behavior<Command> {
        val name = msg.name

        tsNameToTicketSeller.remove(name)

        return Behaviors.same()
    }

    private fun findTicketSellerThen(name: String,f: (ActorRef<TicketSeller.Companion.Command>) -> Unit, ifEmpty: () -> Unit) {
        val ticketSeller = tsNameToTicketSeller[name]
        if (ticketSeller != null) {
            f(ticketSeller)
        } else {
            ifEmpty()
        }
    }

    companion object {
        val name = "boxOffice"

        fun create(timeout: Duration): Behavior<Command> = Behaviors.setup { BoxOffice(it, timeout) }

        sealed interface Command
        data class CreateEvent(val name: String, val tickets: Int, val replyTo: ActorRef<EventResponse>) : Command
        data class GetEvent(val name: String, val replyTo: ActorRef<Optional<Event>>) : Command
        data class GetEvents(val replyTo: ActorRef<Events>) : Command
        data class GetTickets(val event: String, val tickets: Int, val replyTo: ActorRef<TicketSeller.Companion.Tickets>) : Command
        data class CancelEvent(val name: String, val replyTo: ActorRef<Optional<Event>>) : Command
        data class TicketSellerTerminated(val name: String) : Command

        data class Event(val name: String, val tickets: Int)
        data class Events(val events: List<Event>)

        sealed interface EventResponse
        data class EventCreated(val event: Event) : EventResponse
        object EventExists : EventResponse
    }
}