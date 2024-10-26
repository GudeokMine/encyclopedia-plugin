

package io.github.lepitar.sample.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin


class SamplePlugin : JavaPlugin(), Listener {
    override fun onEnable() {
        logger.info("Hello world!")

        println(ServerPlayer::class.java)
        for (declaredField in Entity::class.java.declaredFields) {
            println(declaredField.name)
        }

        server.pluginManager.registerEvents(this, this)
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

        val itemPaletteGUI = ItemPaletteGUI.Builder("도감")
            .show(Material::isFlammable)
            .asItem(this::getDisplayItem)
            .build()

        val player = event.player

        itemPaletteGUI.show(player)
    }

    private fun getDisplayItem(material: Material): GuiItem {
        val item = ItemStack(material)

        return GuiItem(item) { event: InventoryClickEvent ->
            val player = event.whoClicked
            player.closeInventory()
            player.inventory.addItem(item)
        }
    }
}
