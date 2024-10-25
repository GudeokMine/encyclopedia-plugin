

package io.github.lepitar.sample.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


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

//        val gui = ChestGui(5, "도감")
//
//        val pages = PaginatedPane(0, 0, 6, 4)
//
//        val nextButton = OutlinePane(4, 5, 2, 1)
//        val previousButton = OutlinePane(0, 5, 2, 1)
//        val achievementButton = OutlinePane(2, 5, 2, 1)
//
//        val nextButtonItem = ItemStack(Material.LIME_STAINED_GLASS_PANE)
//        val nextButtonMeta = nextButtonItem.itemMeta
//        nextButtonMeta.displayName(Component.text(">>"))
//        nextButtonItem.itemMeta = nextButtonMeta
//        nextButton.addItem(GuiItem(nextButtonItem))
//
//        val previousButtonItem = ItemStack(Material.LIME_STAINED_GLASS_PANE)
//        val previousButtonMeta = previousButtonItem.itemMeta
//        previousButtonMeta.displayName(Component.text("<<"))
//        previousButtonItem.itemMeta = previousButtonMeta
//        previousButton.addItem(GuiItem(previousButtonItem))
//
//        val achievementButtonItem = ItemStack(Material.YELLOW_STAINED_GLASS_PANE)
//        val achievementButtonMeta = achievementButtonItem.itemMeta
//        achievementButtonMeta.displayName(Component.text("도감 달성률 ${0}%"))
//        achievementButtonItem.itemMeta = achievementButtonMeta
//        achievementButton.addItem(GuiItem(achievementButtonItem))
//
//        nextButton.setRepeat(true)
//        previousButton.setRepeat(true)
//        achievementButton.setRepeat(true)
//
//        gui.addPane(nextButton)
//        gui.addPane(previousButton)
//        gui.addPane(achievementButton)

        val player = event.player

        val itemPaletteGUI = ItemPaletteGUI.Builder("test").show(Material::isFlammable).`as`(this::getDisplayItem).build()
        itemPaletteGUI.show(player)
    }

    private fun getDisplayItem(material: Material): GuiItem {
        val item = ItemStack(material)

        return GuiItem(item) { event: InventoryClickEvent ->
            val player = event.whoClicked
            player.closeInventory()
            player.inventory.addItem(item)
            player.sendMessage("Don't get close to ${material}!")
        }
    }
}
