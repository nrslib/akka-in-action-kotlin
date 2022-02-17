package aia.persistence.sharded

import aia.persistence.rest.ShopperServiceSupport
import akka.actor.typed.Props

fun main(args: Array<String>) {
    val system = ShopperServiceSupport.startService { it.spawn(ShardedShoppers.create(), ShardedShoppers.name(), Props.empty()) }

}