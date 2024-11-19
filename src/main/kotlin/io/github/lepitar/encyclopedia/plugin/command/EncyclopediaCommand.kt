package io.github.lepitar.encyclopedia.plugin.command

import io.github.lepitar.encyclopedia.plugin.gui.EncyclopediaGUI
import io.github.lepitar.encyclopedia.plugin.item.ItemCollectionManager
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class EncyclopediaCommand(private val collectionManager: ItemCollectionManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return false
        }

        EncyclopediaGUI.Builder("<shift:-8><glyph:encyclopedia>")
            .asItem(Material::isItem)
            .forPlayer(sender)
            .withCollectionManager(collectionManager)
            .build()
            .show(sender)

        return true
    }
}
