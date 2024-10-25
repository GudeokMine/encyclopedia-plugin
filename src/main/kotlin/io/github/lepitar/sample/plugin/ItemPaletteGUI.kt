package io.github.lepitar.sample.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Orientable.Orientation
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority
import io.github.lepitar.sample.plugin.InventoryUtils.createRectangle
import io.github.lepitar.sample.plugin.InventoryUtils.createWall
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*
import java.util.function.*
import java.util.function.Function
import java.util.stream.Collectors.toCollection


class ItemPaletteGUI private constructor(builder: Builder) : ChestGui(6, builder.title) {
    private val itemFilter: Predicate<Material>?
    private val itemTransformer: Function<Material, GuiItem>?
    private var itemsPane: PaginatedPane? = null

    init {
        this.itemTransformer = builder.itemTransformer
        this.itemFilter = builder.itemFilter

        setOnTopClick { event: InventoryClickEvent ->
            event.isCancelled =
                true
        }
        addPane(createItemsPane().also { this.itemsPane = it })
        addPane(createControlPane())
        addPane(createRectangle(Priority.LOWEST, 1, 5, 7, 1, GuiItem(createWall(Material.BLACK_STAINED_GLASS_PANE))))
        update()
    }


    /*
     * Panes
     */
    private fun createControlPane(): Pane {
        val pane = OutlinePane(0, 5, 9, 1, Priority.LOW)
        pane.orientation = Orientation.HORIZONTAL
        pane.gap = 7

        pane.addItem(PageController.PREVIOUS.toItemStack(this, "Back", this.itemsPane))
        pane.addItem(PageController.NEXT.toItemStack(this, "Next", this.itemsPane))

        return pane
    }

    private fun createItemsPane(): PaginatedPane {
        val itemsToDisplay: Deque<GuiItem> = Arrays.stream(Material.values())
            .filter { material -> !material.isAir }
            .filter(this.itemFilter)
            .map(this.itemTransformer)
            .collect(toCollection { LinkedList() })

        val pane = PaginatedPane(0, 0, 9, 6, Priority.LOWEST)

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
        val page = OutlinePane(0, 0, 9, 6, Priority.LOWEST)
        page.orientation = Orientation.HORIZONTAL

        for (i in 1..ITEMS_PER_PAGE) {
            if (!items.isEmpty())  //a little trick to avoid NoSuchElementException at the last page
                page.addItem(items.removeFirst())
        }

        return page
    }


    private enum class PageController
        (
        private val skullName: String,
        private val shouldContinue: BiPredicate<Int, PaginatedPane?>,
        private val nextPageSupplier: IntUnaryOperator
    ) {
        PREVIOUS("MHF_ArrowLeft",
            BiPredicate { page: Int, itemsPane: PaginatedPane? -> page > 0 },
            IntUnaryOperator { page: Int ->
                var page = page
                --page
            }),
        NEXT("MHF_ArrowRight",
            BiPredicate { page: Int, itemsPane: PaginatedPane? -> page < (itemsPane!!.pages - 1) },
            IntUnaryOperator { page: Int ->
                var page = page
                ++page
            });

        @Suppress("deprecation")
        fun toItemStack(gui: ChestGui, itemName: String?, itemsPane: PaginatedPane?): GuiItem {
            val item = ItemStack(Material.PLAYER_HEAD)
            val meta = item.itemMeta as SkullMeta
            meta.setDisplayName(itemName)
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(this.skullName))
            item.setItemMeta(meta)

            return GuiItem(item, Consumer { event: InventoryClickEvent? ->
                val currentPage = itemsPane!!.page
                if (!shouldContinue.test(currentPage, itemsPane)) return@Consumer

                itemsPane.page = nextPageSupplier.applyAsInt(currentPage)
                gui.update()
            })
        }
    }


    class Builder
        (var title: String) {
        var itemTransformer: Function<Material, GuiItem>? = null
        var itemFilter: Predicate<Material>? = null

        fun `as`(itemTransformer: Function<Material, GuiItem>?): Builder {
            this.itemTransformer = itemTransformer
            return this
        }

        fun show(itemFilter: Predicate<Material>?): Builder {
            this.itemFilter = itemFilter
            return this
        }

        fun build(): ItemPaletteGUI {
            return ItemPaletteGUI(this)
        }
    }

    companion object {
        private const val ITEMS_PER_PAGE = 9 * 5
    }
}