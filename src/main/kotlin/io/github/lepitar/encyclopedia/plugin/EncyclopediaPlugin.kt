package io.github.lepitar.encyclopedia.plugin

import io.github.lepitar.encyclopedia.plugin.gui.EncyclopediaGUI
import io.github.lepitar.encyclopedia.plugin.item.ItemCollectionManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin


class EncyclopediaPlugin : JavaPlugin(), Listener {
    private lateinit var collectionManager: ItemCollectionManager

    override fun onEnable() {
        collectionManager = ItemCollectionManager(this)
        collectionManager.loadCollections()

        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        collectionManager.saveCollections()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val item = event.item ?: return
        if (item.type != Material.BOOK) {
            return
        }

        val player = event.player
        EncyclopediaGUI.Builder("도감")
            .asItem(Material::isFlammable)
            .forPlayer(player)
            .withCollectionManager(collectionManager)
            .build()
            .show(player)
    }
}
