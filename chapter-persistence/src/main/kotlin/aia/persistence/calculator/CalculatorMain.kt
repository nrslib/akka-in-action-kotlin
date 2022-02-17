package aia.persistence.calculator

import aia.persistence.serialization.KotlinModuleJacksonObjectMapperFactory
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.serialization.jackson.JacksonObjectMapperProviderSetup

fun main(args: Array<String>) {
    val setup = ActorSystemSetup.empty().withSetup(
        JacksonObjectMapperProviderSetup(KotlinModuleJacksonObjectMapperFactory())
    )
    // Suppress warning: The name of the actor system is the value of application.conf (akka.cluster.seed-nodes).
    val calculator = ActorSystem.create(Calculator.create(Calculator.name), "shoppers", setup)

    calculator.tell(Calculator.Add(1.0))
    calculator.tell(Calculator.Multiply(3.0))
    calculator.tell(Calculator.Divide(4.0))
    calculator.tell(Calculator.Clear)
    calculator.tell(Calculator.Add(123.0))
    calculator.tell(Calculator.PrintResult)
}

