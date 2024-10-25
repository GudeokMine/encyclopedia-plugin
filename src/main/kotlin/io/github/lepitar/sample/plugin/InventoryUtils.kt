package io.github.lepitar.sample.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack


object InventoryUtils {
    fun createWall(material: Material?): ItemStack {
        val item = ItemStack(material!!)
        val meta = item.itemMeta
        meta.setDisplayName("")
        item.setItemMeta(meta)
        return item
    }

    fun createRectangle(priority: Priority?, x: Int, y: Int, length: Int, height: Int, item: GuiItem?): Pane {
        val pane = OutlinePane(x, y, length, height, priority!!)
        pane.addItem(item!!)
        pane.setRepeat(true)

        return pane
    }
}
