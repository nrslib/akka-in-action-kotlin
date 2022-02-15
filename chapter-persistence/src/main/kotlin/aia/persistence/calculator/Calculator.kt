package aia.persistence.calculator

import aia.persistence.serialization.JacksonSerializable
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior

class Calculator(persistentId: String) :
    EventSourcedBehavior<Calculator.Command, Calculator.Event, Calculator.CalculationResult>(
        PersistenceId.ofUniqueId(persistentId)
    ) {
    companion object {
        val name = "my-calculator"

        fun create(persistentId: String): Behavior<Command> {
            return Behaviors.setup {
                Calculator(persistentId)
            }
        }
    }

    sealed interface Command
    object Clear : Command
    data class Add(val value: Double) : Command
    data class Subtract(val value: Double) : Command
    data class Divide(val value: Double) : Command
    data class Multiply(val value: Double) : Command
    object PrintResult : Command
    data class GetResult(val replyTo: ActorRef<CalculationResult>) : Command

    sealed interface Event : JacksonSerializable
    object Reset : Event
    data class Added(val value: Double) : Event
    data class Subtracted(val value: Double) : Event
    data class Divided(val value: Double) : Event
    data class Multiplied(val value: Double) : Event

    data class CalculationResult(val result: Double = 0.0) : JacksonSerializable {
        fun reset() = copy(result = 0.0)
        fun add(value: Double) = copy(result = this.result + value)
        fun subtract(value: Double) = copy(result = this.result - value)
        fun divide(value: Double) = copy(result = this.result / value)
        fun multiply(value: Double) = copy(result = this.result * value)
    }

    override fun emptyState(): CalculationResult {
        return CalculationResult(0.0)
    }

    override fun shouldSnapshot(state: CalculationResult, event: Event, sequenceNr: Long): Boolean {
        return event is Reset
    }

    override fun commandHandler(): CommandHandler<Command, Event, CalculationResult> {
        return newCommandHandlerBuilder()
            .forAnyState()
            .onCommand(Add::class.java) { _, (value) ->
                Effect().persist(Added(value))
            }
            .onCommand(Subtract::class.java) { _, (value) ->
                Effect().persist(Subtracted(value))
            }
            .onCommand(Divide::class.java) { _, (value) ->
                Effect().persist(Divided(value))
            }
            .onCommand(Multiply::class.java) { _, (value) ->
                Effect().persist(Multiplied(value))
            }
            .onCommand(PrintResult::class.java) { state, _ ->
                Effect().none().thenRun {
                    println("the result is: $state")
                }
            }
            .onCommand(GetResult::class.java) { state, command ->
                Effect().none().thenReply(command.replyTo) {
                    state
                }
            }
            .onCommand(Clear::class.java) { _, _ ->
                Effect().persist(Reset)
            }
            .build()
    }

    override fun eventHandler(): EventHandler<CalculationResult, Event> {
        return newEventHandlerBuilder()
            .forAnyState()
            .onEvent(Reset::class.java) { state, _ ->
                state.reset()
            }
            .onEvent(Added::class.java) { state, (value) ->
                state.add(value)
            }
            .onEvent(Subtracted::class.java) { state, (value) ->
                state.subtract(value)
            }
            .onEvent(Divided::class.java) { state, (value) ->
                state.divide(value)
            }
            .onEvent(Multiplied::class.java) { state, (value) ->
                state.multiply(value)
            }
            .build()
    }
}