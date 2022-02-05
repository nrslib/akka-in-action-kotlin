package com.goticks

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.javadsl.Http
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import java.time.Duration


fun main(args: Array<String>) {
    // for kotlin jackson
    val objectMapper = ObjectMapper().registerKotlinModule()

    val config = ConfigFactory.load()
    val host = config.getString("http.host") // Gets the host and a port from the configuration
    val port = config.getInt("http.port")

    val system = ActorSystem.create(create(objectMapper, host, port), "routes")

//    val api = RestApi(system.executionContext, system, objectMapper, Duration.ofSeconds(5)).createRoute() // the RestApi provides a Route
//
//    val materializer = Materializer.createMaterializer(system)
//    Http.get(system).newServerAt(host, port).bind(api) // Starts the HTTP server
}

fun create(objectMapper: ObjectMapper, host: String, port: Int): Behavior<Void> {
    return Behaviors.setup { context ->
        val api = RestApi(context, objectMapper, Duration.ofSeconds(5))
        val route = api.createRoute()
        Http.get(context.system()).newServerAt(host, port).bind(route)

        Behaviors.empty()
    }
}

