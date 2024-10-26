package io.github.lepitar.sample.plugin

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Orientable.Orientation
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
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
            event.isCancelled = true
        }
        addPane(createItemsPane().also { this.itemsPane = it })
        addPane(createControlPane())
        update()
    }


    /*
     * Panes
     */
    private fun createControlPane(): Pane {
        val pane = StaticPane(0, 5, 6, 1, Priority.LOW)

        val previousButton = PageController.PREVIOUS.toItemStack(this, "<<", this.itemsPane)
        pane.addItem(previousButton, 0, 0)
        pane.addItem(previousButton, 1, 0)

        val nextButton = PageController.NEXT.toItemStack(this, ">>", this.itemsPane)
        pane.addItem(nextButton, 4, 0)
        pane.addItem(nextButton, 5, 0)

        return pane
    }

    private fun createItemsPane(): PaginatedPane {
        val itemsToDisplay: Deque<GuiItem> = Arrays.stream(Material.values())
            .filter(this.itemFilter)
            .map(this.itemTransformer)
            .collect(toCollection { LinkedList() })

        val pane = PaginatedPane(0, 0, 6, 5, Priority.LOWEST)

        var i = 0
        val pagesAmount = (itemsToDisplay.size / ITEMS_PER_PAGE)
        do {
            pane.addPane(i, createPage(itemsToDisplay))
            i++
        } while (i < pagesAmount)

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
        private val shouldContinue: BiPredicate<Int, PaginatedPane?>,
        private val nextPageSupplier: IntUnaryOperator
    ) {
        PREVIOUS(
            BiPredicate { page: Int, _: PaginatedPane? -> page > 0 },
            IntUnaryOperator { page: Int ->
                var page = page
                --page
            }),
        NEXT(
            BiPredicate { page: Int, itemsPane: PaginatedPane? -> page < (itemsPane!!.pages - 1) },
            IntUnaryOperator { page: Int ->
                var page = page
                ++page
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


    class Builder
        (var title: String) {
        var itemTransformer: Function<Material, GuiItem>? = null
        var itemFilter: Predicate<Material>? = null

        fun asItem(itemTransformer: Function<Material, GuiItem>?): Builder {
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