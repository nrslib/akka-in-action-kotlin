package aia.persistence

import aia.persistence.rest.ShopperServiceSupport
import akka.actor.typed.Props

fun main(args: Array<String>) {
    ShopperServiceSupport.startService { it.spawn(ShoppersSingleton.create(), ShoppersSingleton.name(), Props.empty()) }
}