package io.github.lepitar.encyclopedia.plugin.item

import org.bukkit.Material

data class ItemDiscovery(
    val material: Material,
    var isDiscovered: Boolean = false
)
