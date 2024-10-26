package io.github.lepitar.encyclopedia.plugin.gui

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Orientable.Orientation
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import io.github.lepitar.encyclopedia.plugin.item.ItemCollectionManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.*
import java.util.function.Function
import java.util.stream.Collectors.toCollection


class EncyclopediaGUI private constructor(builder: Builder) : ChestGui(6, builder.title) {
    private val itemFilter: Predicate<Material>?
    private val itemTransformer: Function<Material, GuiItem>?
    private var itemsPane: PaginatedPane? = null
    private val player: Player
    private val collectionManager: ItemCollectionManager

    init {
        this.player = builder.player
        this.collectionManager = builder.collectionManager
        this.itemTransformer = builder.itemTransformer ?: Function { material ->
            createItemGuiItem(material)
        }
        this.itemFilter = builder.itemFilter

        setOnTopClick { event: InventoryClickEvent ->
            event.isCancelled = true
        }
        addPane(createItemsPane().also { this.itemsPane = it })
        addPane(createControlPane())
        addPane(createStatsPane())
        update()
    }


    /*
     * Panes
     */
    private fun createControlPane(): Pane {
        val pane = StaticPane(0, 5, 6, 1, Priority.LOW)

        val previousButton = PageController.PREVIOUS.toItemStack(this, "§f<<", this.itemsPane)
        pane.addItem(previousButton, 0, 0)
        pane.addItem(previousButton, 1, 0)

        val nextButton = PageController.NEXT.toItemStack(this, "§f>>", this.itemsPane)
        pane.addItem(nextButton, 4, 0)
        pane.addItem(nextButton, 5, 0)

        return pane
    }

    private fun createItemsPane(): PaginatedPane {
        val itemsToDisplay: Deque<GuiItem> = Arrays.stream(Material.values())
            .filter { material -> !material.isAir }
            .filter(this.itemFilter)
            .map(this.itemTransformer)
            .collect(toCollection { LinkedList() })

        val pane = PaginatedPane(0, 0, 6, 5, Priority.LOWEST)

        var i = 0
        val pagesAmount = (itemsToDisplay.size / ITEMS_PER_PAGE) + 1

        while (i < pagesAmount) {
            pane.addPane(i, createPage(itemsToDisplay))
            i++
        }

        pane.page = 0

        return pane
    }

    private fun createPage(items: Deque<GuiItem>): Pane {
        val page = OutlinePane(0, 0, 6, 5, Priority.LOWEST)
        page.orientation = Orientation.HORIZONTAL

        for (i in 1..ITEMS_PER_PAGE) {
            if (!items.isEmpty())  //a little trick to avoid NoSuchElementException at the last page
                page.addItem(items.removeFirst())
        }

        return page
    }

    private fun createStatsPane(): Pane {
        val pane = OutlinePane(2, 5, 2, 1, Priority.LOW)
        val collection = collectionManager.getCollection(player)

        val discoveredCount = Material.values().count { collection.isDiscovered(it) }
        val totalCount = Material.values().count { itemFilter?.test(it) ?: true }

        val statsItem = ItemStack(Material.BOOK)
        val meta = statsItem.itemMeta

        meta.displayName(Component.text("§6도감 진행도"))
        meta.lore(
            listOf(
                Component.text("§7발견 : $discoveredCount/$totalCount"),
                Component.text("§7달성률 : ${String.format("%.0f", discoveredCount.toDouble() / totalCount * 100)}%")
            )
        )
        statsItem.itemMeta = meta

        pane.addItem(GuiItem(statsItem))
        pane.setRepeat(true)

        return pane
    }

    private fun createItemGuiItem(material: Material): GuiItem {
        val collection = collectionManager.getCollection(player)
        val isDiscovered = collection.isDiscovered(material)

        val item = ItemStack(material)
        val meta = item.itemMeta

        meta.displayName(
            if (isDiscovered) {
                Component.text("§f${material.name}")
            } else {
                Component.text("§7- ??? -")
            }
        )

        item.itemMeta = meta

        return GuiItem(item) { event ->
            event.isCancelled = true

            if (collection.isDiscovered(material)) {
                return@GuiItem
            }

            if (!event.click.isRightClick || event.currentItem?.isSimilar(item) != true) {
                return@GuiItem
            }

            collection.discover(material)

            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f)
            player.sendMessage("§a새로운 아이템을 발견했습니다: §f${material.name}")

            meta.displayName(Component.text("§f${material.name}"))
            item.itemMeta = meta
            update()
        }
    }


    private enum class PageController
        (
        private val shouldContinue: BiPredicate<Int, PaginatedPane?>,
        private val nextPageSupplier: IntUnaryOperator
    ) {
        PREVIOUS(
            BiPredicate { page: Int, _: PaginatedPane? -> page > 0 },
            IntUnaryOperator { page: Int ->
                var currentPage = page
                --currentPage
            }),
        NEXT(
            BiPredicate { page: Int, itemsPane: PaginatedPane? -> page < (itemsPane!!.pages - 1) },
            IntUnaryOperator { page: Int ->
                var currentPage = page
                ++currentPage
            });

        fun toItemStack(gui: ChestGui, itemName: String?, itemsPane: PaginatedPane?): GuiItem {
            val item = ItemStack(Material.LIME_STAINED_GLASS_PANE)
            val meta = item.itemMeta
            meta.displayName(Component.text(itemName.toString()))
            item.setItemMeta(meta)

            return GuiItem(item, Consumer { _: InventoryClickEvent? ->
                val currentPage = itemsPane!!.page
                if (!shouldContinue.test(currentPage, itemsPane)) return@Consumer

                itemsPane.page = nextPageSupplier.applyAsInt(currentPage)
                gui.update()
            })
        }
    }


    class Builder(var title: String) {
        var itemTransformer: Function<Material, GuiItem>? = null
        var itemFilter: Predicate<Material>? = null
        lateinit var player: Player
        lateinit var collectionManager: ItemCollectionManager

        fun asItem(itemFilter: Predicate<Material>): Builder {
            this.itemFilter = itemFilter
            return this
        }

        fun forPlayer(player: Player): Builder {
            this.player = player
            return this
        }

        fun withCollectionManager(manager: ItemCollectionManager): Builder {
            this.collectionManager = manager
            return this
        }

        fun build(): EncyclopediaGUI {
            return EncyclopediaGUI(this)
        }
    }


    companion object {
        private const val ITEMS_PER_PAGE = 6 * 5
    }
}