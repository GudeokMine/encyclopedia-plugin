package io.github.lepitar.encyclopedia.plugin.item

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class ItemCollectionManager(private val plugin: JavaPlugin) {
    private val collections: MutableMap<UUID, PlayerItemCollection> = HashMap()

    fun getCollection(player: Player): PlayerItemCollection {
        return collections.getOrPut(player.uniqueId) { PlayerItemCollection() }
    }
    
    fun saveCollections() {
        val config = YamlConfiguration()
        collections.forEach { (uuid, collection) ->
            config.set(uuid.toString(), collection.serialize())
        }
        config.save(File(plugin.dataFolder, "collections.yml"))
    }

    fun loadCollections() {
        val file = File(plugin.dataFolder, "collections.yml")
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)
        config.getKeys(false).forEach { uuid ->
            val collection = PlayerItemCollection()
            @Suppress("UNCHECKED_CAST")
            collection.deserialize(
                config.getConfigurationSection(uuid)?.getValues(false) as? Map<String, Boolean> ?: mapOf()
            )
            collections[UUID.fromString(uuid)] = collection
        }
    }
}