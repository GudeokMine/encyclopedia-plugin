package io.github.lepitar.sample.plugin

import org.bukkit.Material
import java.util.*

class PlayerItemCollection {
    private val discoveries: MutableMap<Material, ItemDiscovery> = EnumMap(Material::class.java)

    init {
        Material.values().forEach { material ->
            discoveries[material] = ItemDiscovery(material)
        }
    }

    fun discover(material: Material) {
        discoveries[material]?.isDiscovered = true
    }

    fun isDiscovered(material: Material): Boolean {
        return discoveries[material]?.isDiscovered ?: false
    }

    fun serialize(): Map<String, Boolean> {
        return discoveries.mapKeys { it.key.name }
            .mapValues { it.value.isDiscovered }
    }

    fun deserialize(data: Map<String, Boolean>) {
        data.forEach { (materialName, discovered) ->
            try {
                val material = Material.valueOf(materialName)
                discoveries[material] = ItemDiscovery(material, discovered)
            } catch (e: IllegalArgumentException) {
                // Skip invalid materials
            }
        }
    }
}