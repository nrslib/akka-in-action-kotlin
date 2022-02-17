package aia.persistence.rest

import aia.persistence.Basket
import aia.persistence.Shopper
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.RecipientRef
import akka.actor.typed.Scheduler
import akka.actor.typed.javadsl.AskPattern
import akka.http.javadsl.Http
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.Directives.*
import akka.http.javadsl.server.PathMatchers.segment
import akka.http.javadsl.server.Route
import java.time.Duration
import java.util.concurrent.CompletionStage

class ShopperService {
    companion object {
        fun startHttpServer(route: Route, system: ActorSystem<*>) {
            val config = system.settings().config()
            val host = config.getString("http.host")
            val port = config.getInt("http.port")

            val futureBinding = Http.get(system).newServerAt(host, port).bind(route)

            futureBinding.whenComplete { binding, exception ->
                if (binding != null) {
                    val address = binding.localAddress();
                    system.log().info("Server online at http://{}:{}/",
                        address.hostString,
                        address.port
                    )
                } else {
                    system.log().error("Failed to bind HTTP endpoint, terminating system", exception);
                    system.terminate();
                }
            }
        }
    }
}

class ShoppersRoutes(system: ActorSystem<*>, private val shoppers: ActorRef<Shopper.Command>) {
    private val askTimeout: Duration = system.settings().config().getDuration("shoppers.ask-timeout")
    private val scheduler: Scheduler = system.scheduler()

    fun routes(): Route = concat(pay(), getBasket())

    private fun pay() =
        post {
            pathPrefix("shopper") {
                path(segment().slash("pay")) { shopperId ->
                    shoppers.tell(Shopper.PayBasket(shopperId.toLong()))
                    complete(StatusCodes.OK)
                }
            }
        }

    private fun getBasket() =
        get {
            pathPrefix("shopper") {
                path(segment().slash("basket")) { shopperId ->
                    onSuccess({ask(shoppers) { replyTo: ActorRef<Shopper.Command> -> Basket.GetItems(shopperId.toLong(), replyTo) }}) {
                        when (it) {
                            is Shopper.BasketGetItemsResponse ->
                                completeOK(it.items, Jackson.marshaller())
                            else ->
                                complete(StatusCodes.NOT_FOUND)
                        }
                    }
                }
            }
        }

    private fun <Req, Res> ask(actor: RecipientRef<Req>, replyFunction: (ActorRef<Res>) -> Req): CompletionStage<Res>? {
        return AskPattern.ask(actor, replyFunction, askTimeout, scheduler)
    }
}
