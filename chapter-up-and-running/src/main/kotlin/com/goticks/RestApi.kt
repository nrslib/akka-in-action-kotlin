package com.goticks

import akka.actor.typed.ActorRef
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.javadsl.AskPattern
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.Directives.*
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.*


class RestApi(override val context: ActorContext<Void>, val objectMapper: ObjectMapper, override val timeout: Duration) : BoxOfficeApi {
    override val boxOffice = context.spawn(BoxOffice.create(timeout), BoxOffice.name, Props.empty())

    fun routes(): Route {
        return concat(
            pathPrefix("events") {
                concat(
                    eventsGetRoute(),
                    pathPrefix(segment().slash("tickets")) {
                        postTicketRoute(it)
                    },
                    pathPrefix(segment()){
                        concat(
                            eventPostRoute(it),
                            eventGetRoute(it),
                            eventDeleteRoute(it)
                        )
                    }
                )
            }
        )
    }

    private fun eventsGetRoute(): Route {
        return get {
            pathEndOrSingleSlash {
                onSuccess({ getEvents() }, {
                    completeOK(it, Jackson.marshaller())
                })
            }
        }
    }

    private fun eventPostRoute(event: String): Route {
        return post {
            entity(Jackson.unmarshaller(objectMapper, EventDescription::class.java)) { eventDescription ->
                val ed = eventDescription!!
                onSuccess(createEvent(event, ed.tickets)) {
                    when(it) {
                        is BoxOffice.Companion.EventCreated -> complete(StatusCodes.CREATED, it.event, Jackson.marshaller())
                        is BoxOffice.Companion.EventExists -> {
                            val err = Error("$event event exists already.")
                            complete(StatusCodes.BAD_REQUEST, err, Jackson.marshaller())
                        }
                    }
                }
            }
        }
    }

    private fun eventGetRoute(event: String): Route {
        return get {
            onSuccess({getEvent(event)}) {
                if (it.isPresent) {
                    completeOK(it.get(), Jackson.marshaller())
                } else {
                    complete(StatusCodes.NOT_FOUND)
                }
            }
        }
    }

    private fun eventDeleteRoute(event: String): Route {
        return delete {
            onSuccess({cancelEvent(event)}) {
                if (it.isPresent) {
                    completeOK(it.get(), Jackson.marshaller())
                } else {
                    complete(StatusCodes.NOT_FOUND)
                }
            }
        }
    }

    private fun postTicketRoute(event: String): Route {
        return post {
            pathEndOrSingleSlash {
                entity(Jackson.unmarshaller(objectMapper, TicketRequest::class.java)) {
                    onSuccess(requestTickets(event, it.tickets)) { tickets ->
                        if (tickets.entries.isEmpty()) {
                            complete(StatusCodes.NOT_FOUND)
                        } else {
                            complete(StatusCodes.CREATED, tickets, Jackson.marshaller())
                        }
                    }
                }
            }
        }
    }

}

interface BoxOfficeApi {
    val boxOffice: ActorRef<BoxOffice.Companion.Command>
    val timeout: Duration
    val context: ActorContext<Void>

    fun createEvent(event: String, nrOrTickets: Int) =
        AskPattern.ask(boxOffice, { replyTo: ActorRef<BoxOffice.Companion.EventResponse> -> BoxOffice.Companion.CreateEvent(event, nrOrTickets, replyTo)}, timeout, context.system().scheduler())

    fun getEvents() =
        AskPattern.ask(boxOffice, { replyTo: ActorRef<BoxOffice.Companion.Events> -> BoxOffice.Companion.GetEvents(replyTo)}, timeout, context.system().scheduler())

    fun getEvent(event: String) =
        AskPattern.ask(boxOffice, { replyTo: ActorRef<Optional<BoxOffice.Companion.Event>> -> BoxOffice.Companion.GetEvent(event, replyTo)}, timeout,  context.system().scheduler())

    fun cancelEvent(event: String) =
        AskPattern.ask(boxOffice, { replyTo: ActorRef<Optional<BoxOffice.Companion.Event>> -> BoxOffice.Companion.CancelEvent(event, replyTo)}, timeout,  context.system().scheduler())

    fun requestTickets(event: String, tickets: Int) =
        AskPattern.ask(boxOffice, { replyTo: ActorRef<TicketSeller.Companion.Tickets> -> BoxOffice.Companion.GetTickets(event, tickets, replyTo)}, timeout,  context.system().scheduler())
}
