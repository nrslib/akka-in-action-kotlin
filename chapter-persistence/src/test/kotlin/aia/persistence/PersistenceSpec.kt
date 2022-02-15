package aia.persistence

import akka.actor.typed.ActorSystem
import org.apache.commons.io.FileUtils
import java.io.File

interface PersistenceSpec<T> {
    companion object {
        val strageLocation = listOf(
            "akka.persistence.journal.leveldb.dir",
            "akka.persistence.journal.leveldb-shared.store.dir",
            "akka.persistence.snapshot-store.local.dir"
        )
    }

    val system: ActorSystem<T>

    fun deleteStorageLocations() {
        strageLocation.map { File(system.settings().config().getString(it)) }
            .forEach {
                try {
                    FileUtils.deleteDirectory(it)
                } catch (_: Exception) {

                }
            }
    }
}