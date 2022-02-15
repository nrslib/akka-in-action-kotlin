package aia.persistence.calculator

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.persistence.testkit.javadsl.EventSourcedBehaviorTestKit
import akka.serialization.SerializationExtension
import akka.serialization.jackson.JacksonObjectMapperProvider
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertEquals

class CalculatorSpec {
    companion object {
        @ClassRule @JvmField
        val testkit: TestKitJunitResource = TestKitJunitResource(EventSourcedBehaviorTestKit.config())
    }

    private val eventSourcedTestKit: EventSourcedBehaviorTestKit<Calculator.Command, Calculator.Event, Calculator.CalculationResult> =
        EventSourcedBehaviorTestKit.create(testkit.system(), Calculator.create(Calculator.name))

    @Before
    fun beforeEach() {
        eventSourcedTestKit.clear()
    }

    @Test
    fun theCalculatorShouldRecoverLastKnownResultAfterClash() {
        TODO()
        eventSourcedTestKit.runCommand(Calculator.Add(10.0))
        assertEquals("a", "a")
    }

}