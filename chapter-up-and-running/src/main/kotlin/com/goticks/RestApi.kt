package com.goticks

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.StatusCode
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.Route
import akka.http.javadsl.server.Directives.*
import akka.http.javadsl.server.PathMatchers.segment
import akka.pattern.Patterns.ask
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.Duration
import kotlin.reflect.cast

class RestApi(val system: ActorSystem, val objectMapper: ObjectMapper, override val timeout: Duration) : BoxOfficeApi {
    override val boxOffice = system.actorOf(BoxOffice.props(timeout), BoxOffice.name)

    fun createRoute(): Route {
        return concat(
            pathPrefix("events") {
                concat(
                    getEventsRoute(),
                    pathPrefix(segment()){name ->
                        concat(
                            postEventsRoute(name)
                        )
                    }
                )
            }
        )
    }

    private fun getEventsRoute(): Route {
        return get {
            pathEndOrSingleSlash {
                onSuccess({ getEvents() }, {
                    completeOK(it, Jackson.marshaller())
                })
            }
        }
    }

    private fun postEventsRoute(event: String): Route {
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
}

interface BoxOfficeApi {
    val boxOffice: ActorRef
    val timeout: Duration

    fun createEvent(event: String, nrOrTickets: Int) =
        ask(boxOffice, BoxOffice.Companion.CreateEvent(event, nrOrTickets), timeout)
            .thenApply { BoxOffice.Companion.EventResponse::class.cast(it) }

    fun getEvents() =
        ask(boxOffice, BoxOffice.Companion.GetEvents, timeout)
            .thenApply { BoxOffice.Companion.Events::class.cast(it) }

    fun getEvent(event: String) =
        ask(boxOffice, BoxOffice.Companion.GetEvent(event), timeout)
            .thenApply { BoxOffice.Companion.GetEvent::class.cast(it) }

    fun cancelEvent(event: String) =
        ask(boxOffice, BoxOffice.Companion.CancelEvent(event), timeout)
            .thenApply { BoxOffice.Companion.CancelEvent::class.cast(it) }

    fun requestTickets(event: String, tickets: Int) =
        ask(boxOffice, BoxOffice.Companion.GetTickets(event, tickets), timeout)
            .thenApply { BoxOffice.Companion.GetTickets::class.cast(it) }
}
