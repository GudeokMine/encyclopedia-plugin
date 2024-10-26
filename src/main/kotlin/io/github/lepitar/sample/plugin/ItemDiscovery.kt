package io.github.lepitar.sample.plugin

import org.bukkit.Material

data class ItemDiscovery(
    val material: Material,
    var isDiscovered: Boolean = false
)
