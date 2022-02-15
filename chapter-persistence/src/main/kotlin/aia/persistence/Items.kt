package aia.persistence

import java.math.BigDecimal
import java.util.*
import kotlin.math.min

data class Items(val list: List<Item>) {
    companion object {
        fun aggregate(list: List<Item>) = Items(add(list))
        private fun add(list: List<Item>) = aggregateIndexed(indexed(list))
        private fun indexed(list: List<Item>): List<Pair<Item, Int>> = list.withIndex().map { it.value to it.index }
        private fun aggregateIndexed(indexed: List<Pair<Item, Int>>): List<Item> {
            fun grouped() = indexed.groupBy { it.first.productId }
            fun reduced() = grouped().map { (_, groupedIndexed) ->
                val init = Optional.empty<Item>() to Int.MAX_VALUE
                val (item, ix) = groupedIndexed.fold(init) { (accItem, accIx), (item, ix) ->
                    val aggregated =
                        accItem.map { i -> item.aggregate(i) }
                            .orElse(Optional.of(item))

                    aggregated to min(accIx, ix)
                }

                item.get() to ix
            }.toMap()

            fun sorted(): List<Item> =
                reduced()
                    .toList()
                    .sortedBy { (_, index) -> index }
                    .map { (item, _) -> item }

            return sorted()
        }
    }

    fun add(newItem: Item) = aggregate(list + newItem)
    fun add(items: Items) = aggregate(list + items.list)

    fun containsProduct(productId: String) = list.any { it.productId == productId }

    fun removeItem(productId: String) = aggregate(list.filterNot { it.productId == productId })

    fun updateItem(productId: String, number: Int): Items {
        val item = list.find { it.productId == productId }
        val newList = if (item != null) {
            list.filterNot { it.productId == productId } + item.update(number)
        } else list
        return Items.aggregate(newList)
    }

    fun clear() = Items(listOf())
}

data class Item(val productId: String, val number: Int, val unitPrice: BigDecimal) {
    fun aggregate(item: Item): Optional<Item> =
        if (item.productId == productId) {
            Optional.of(copy(number = number + item.number))
        } else {
            Optional.empty()
        }

    fun update(number: Int): Item = copy(number = number)
}