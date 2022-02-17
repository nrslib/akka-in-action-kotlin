package aia.persistence.rest

import aia.persistence.Shopper
import aia.persistence.serialization.KotlinModuleJacksonObjectMapperFactory
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import akka.serialization.jackson.JacksonObjectMapperProviderSetup

object ShopperServiceSupport {
    fun startService(spawnShopperRoot :(context: ActorContext<*>) -> ActorRef<Shopper.Command>): ActorSystem<*> {
        val setup = ActorSystemSetup.empty().withSetup(
            JacksonObjectMapperProviderSetup(KotlinModuleJacksonObjectMapperFactory())
        )

        val rootBehavior = Behaviors.setup<Void> {
            val shopper = spawnShopperRoot(it)
            val shopperRoutes = ShoppersRoutes(it.system(), shopper)
            ShopperService.startHttpServer(shopperRoutes.routes(), it.system())

            Behaviors.empty()
        }

        // The ActorSystem name is the value found in the configuration file (akka.cluster.seed-nodes).
        return ActorSystem.create(rootBehavior, "shoppers", setup)
    }
}