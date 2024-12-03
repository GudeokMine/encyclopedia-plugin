package io.github.lepitar.encyclopedia.plugin

import io.github.lepitar.encyclopedia.plugin.command.EncyclopediaCommand
import io.github.lepitar.encyclopedia.plugin.item.ItemCollectionManager
import org.bukkit.plugin.java.JavaPlugin

class EncyclopediaPlugin : JavaPlugin() {
    private lateinit var collectionManager: ItemCollectionManager

    override fun onEnable() {
        collectionManager = ItemCollectionManager(this)
        collectionManager.loadCollections()

        getCommand("도감")?.setExecutor(EncyclopediaCommand(collectionManager))
    }
    
    override fun onDisable() {
        collectionManager.saveCollections()
    }
}
