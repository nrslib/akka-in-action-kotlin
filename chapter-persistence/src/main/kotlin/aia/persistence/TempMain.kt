package aia.persistence

import aia.persistence.serialization.KotlinModuleJacksonObjectMapperFactory
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.serialization.jackson.JacksonObjectMapperProviderSetup
import java.math.BigDecimal

fun main(args: Array<String>) {
    val setup = ActorSystemSetup.empty().withSetup(
        JacksonObjectMapperProviderSetup(KotlinModuleJacksonObjectMapperFactory())
    )
    val system = ActorSystem.create(Behaviors.setup<Void> {
        val macbookPro =
            Item("Apple Macbook Pro", 1, BigDecimal("2499.99"))
        val macPro = Item("Apple Mac Pro", 1, BigDecimal("10499.99"))
        val displays = Item("4K Display", 3, BigDecimal("2499.99"))
        val appleMouse = Item("Apple Mouse", 1, BigDecimal("99.99"))
        val appleKeyboard = Item("Apple Keyboard", 1, BigDecimal("79.99"))

        val shoppers = it.spawn(LocalShoppers.create(), LocalShoppers.name, Props.empty())
        val printer = it.spawn(Behaviors.setup<Shopper.Command> { Printer(it) }, "printer", Props.empty())

        val shopperId1 = 8L
        val shopperId2 = 9L
        shoppers.tell(Basket.Add(appleMouse, shopperId1))
        shoppers.tell(Basket.GetItems(shopperId1, printer))

        Behaviors.empty()
    }, LocalShoppers.name, setup)
}

class Printer(context: ActorContext<Shopper.Command>?) : AbstractBehavior<Shopper.Command>(context) {
    override fun onMessage(msg: Shopper.Command): Behavior<Shopper.Command> =
        when (msg) {
            is Shopper.BasketGetItemsResponse -> {
                println("--- start ---")
                msg.items.list.forEach {
                    println(it)
                }
                println("--- end ---")
                this
            }
            else -> {
                this
            }
        }

}