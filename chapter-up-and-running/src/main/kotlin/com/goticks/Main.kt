package com.goticks

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.stream.Materializer
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

    val system = ActorSystem.create()
    // comment out; this code is for implicit parameter. you don't need.
    // val ec = system.dispatcher // bindingFuture.map requires an implicit ExecutionContext
    val api = RestApi(system, objectMapper, Duration.ofSeconds(5)).createRoute() // the RestApi provides a Route

    val materializer = Materializer.createMaterializer(system)
    Http.get(system).newServerAt(host, port).bind(api) // Starts the HTTP server
}
