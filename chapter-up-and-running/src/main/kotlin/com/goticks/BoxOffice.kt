package com.goticks

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import akka.pattern.Patterns.ask
import akka.pattern.Patterns.pipe
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlin.streams.toList

class BoxOffice(private val timeout: Duration) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private fun createTicketSeller(name: String): ActorRef {
        return context.actorOf(TicketSeller.props(name), name)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(CreateEvent::class.java, this::createEvent)
            .match(GetTickets::class.java, this::getTickets)
            .match(GetEvent::class.java, this::getEvent)
            .match(GetEvents::class.java, this::getEvents)
            .match(CancelEvent::class.java, this::cancelEvent)
            .build()
    }

    fun createEvent(event: CreateEvent) {
        val name = event.name
        val tickets = event.tickets

        fun create() {
            val eventTickets = createTicketSeller(name)

            val newTickets = (1..tickets).map { TicketSeller.Companion.Ticket(it) }.toList()
            eventTickets.tell(TicketSeller.Companion.Add(newTickets), self)
            sender.tell(EventCreated(Event(name, tickets)), self)
        }

        context.child(name).fold(::create) { sender.tell(EventExists, self) }
    }

    fun getTickets(getTickets: GetTickets) {
        val event = getTickets.event
        val tickets = getTickets.tickets

        fun notFound() = sender.tell(TicketSeller.Companion.Tickets(event), self)
        fun buy(child: ActorRef) = child.forward(TicketSeller.Companion.Buy(tickets), context)

        context.child(event).fold(::notFound, ::buy)
    }

    fun getEvent(getEvent: GetEvent) {
        val event = getEvent.name

        fun notFound() = sender.tell(null, self)
        fun getEvent(child: ActorRef) = child.forward(TicketSeller.Companion.GetEvent, context)
        context.child(event).fold(::notFound, ::getEvent)
    }

    fun getEvents(getEvents: GetEvents) {
        fun getEventFutures() = context.children.map {
            ask(self, GetEvent(it.path().name()), timeout)
                .thenApply { it as Event? }
                .toCompletableFuture()
        }

        val futures = getEventFutures()
        val futureEvents: CompletionStage<Events> = CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply {
                val events = futures.stream()
                    .map { it.join() }
                    .filter { it != null }
                    .map { it!! }
                    .toList()
                Events(events)
            }

        pipe(futureEvents, context.dispatcher).to(sender)
    }

    fun cancelEvent(cancelEvent: CancelEvent) {
        val event = cancelEvent.name

        fun notFound() = sender.tell(null, self)
        fun cancelEvent(child: ActorRef) = child.forward(TicketSeller.Companion.Cancel, context)
        context.child(event).fold(::notFound, ::cancelEvent)
    }

    companion object {
        fun props(timeout: Duration): Props {
            return Props.create(BoxOffice::class.java) { BoxOffice(timeout)}
        }
        val name = "boxOffice"

        data class CreateEvent(val name: String, val tickets: Int)
        data class GetEvent(val name: String)
        object GetEvents
        data class GetTickets(val event: String, val tickets: Int)
        data class CancelEvent(val name: String)

        data class Event(val name: String, val tickets: Int)
        data class Events(val events: List<Event>)

        sealed interface EventResponse
        data class EventCreated(val event: Event) : EventResponse
        object EventExists : EventResponse
    }
}