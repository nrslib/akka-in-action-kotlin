package aia.persistence.calculator

import akka.actor.typed.ActorSystem

fun main(args: Array<String>) {
    val calculator = ActorSystem.create(Calculator.create(Calculator.name), Calculator.name)

    calculator.tell(Calculator.Add(1.0))
    calculator.tell(Calculator.Multiply(3.0))
    calculator.tell(Calculator.Divide(4.0))
    calculator.tell(Calculator.PrintResult)
}
