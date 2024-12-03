package io.github.lepitar.encyclopedia.plugin.gui

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.*
import com.github.stefvanschie.inventoryframework.pane.Orientable.Orientation
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority
import com.github.stefvanschie.inventoryframework.pane.util.Pattern
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
    private val itemFilter = builder.itemFilter ?: Predicate { true }
    private val itemTransformer = builder.itemTransformer ?: Function(::createItemGui)
    private val player: Player = builder.player
    private val collectionManager: ItemCollectionManager = builder.collectionManager
    private var itemsPane: PaginatedPane? = null
    private var controlPane: Pane? = null
    private var statsPane: Pane? = null
    private var currentTab = ItemTab.ALL

    init {
        setOnTopClick { it.isCancelled = true }
        initializePanes()
        update()
    }

    private fun initializePanes() {
        itemsPane = createItemsPane()
        controlPane = createControlPane()
        statsPane = createStatsPane()
        addPane(itemsPane!!)
        addPane(controlPane!!)
        addPane(statsPane!!)
        addPane(createTabPane())
    }

    private fun createItemGui(material: Material): GuiItem {
        val collection = collectionManager.getCollection(player)
        val item = ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                displayName(Component.text(if (collection.isDiscovered(material)) "§f${material.name}" else "§7- ??? -"))
            }
        }

        return GuiItem(item) { event ->
            event.isCancelled = true

            if (collection.isDiscovered(material) || !event.isRightClick || player.itemOnCursor.type.name != item.type.name) {
                return@GuiItem
            }

            collection.discover(material)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f)
            player.sendMessage("§a새로운 아이템을 발견했습니다: §f${material.name}")

            val meta = item.itemMeta
            meta.displayName(Component.text("§f${material.name}"))
            item.setItemMeta(meta)

            panes.remove(statsPane)
            statsPane = createStatsPane()
            addPane(statsPane!!)

            update()
        }
    }

    private fun createItemsPane(): PaginatedPane {
        val itemsToDisplay: Deque<GuiItem> = Arrays.stream(Material.values())
            .filter { material -> !material.isAir }
            .filter(this.itemFilter)
            .filter(currentTab.itemFilter)
            .map(this.itemTransformer)
            .collect(toCollection { LinkedList() })

        val pane = PaginatedPane(0, 0, 5, 5, Priority.LOWEST)

        val pagesAmount = (itemsToDisplay.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
        repeat(pagesAmount) {
            pane.addPane(it, createPage(itemsToDisplay))
        }

        pane.page = 0
        return pane
    }

    private fun createPage(items: Deque<GuiItem>): Pane {
        val page = OutlinePane(1, 0, 5, 5, Priority.LOWEST)
        page.orientation = Orientation.HORIZONTAL

        repeat(ITEMS_PER_PAGE) {
            if (items.isNotEmpty()) {
                page.addItem(items.removeFirst())
            }
        }

        return page
    }

    private fun createControlPane(): Pane {
        val pane = StaticPane(0, 5, 6, 1, Priority.LOW)

        val previousButton = PageController.PREVIOUS.toItemStack(this, "§f<<", this.itemsPane)
        pane.addItem(previousButton, 1, 0)
        pane.addItem(previousButton, 2, 0)

        val nextButton = PageController.NEXT.toItemStack(this, "§f>>", this.itemsPane)
        pane.addItem(nextButton, 4, 0)
        pane.addItem(nextButton, 5, 0)

        return pane
    }

    private fun createStatsPane(): Pane {
        val pane = OutlinePane(3, 5, 1, 1, Priority.LOW)
        val collection = collectionManager.getCollection(player)

        val filteredMaterials = Arrays.stream(Material.values())
            .filter { !it.isAir }
            .filter(currentTab.itemFilter)
            .filter(itemFilter)
            .collect(toCollection { LinkedList() })

        val discoveredCount = filteredMaterials.count { collection.isDiscovered(it) }
        val totalCount = filteredMaterials.size

        val statsItem = ItemStack(Material.PAPER).apply {
            itemMeta = itemMeta?.apply {
                setCustomModelData(17741)
                displayName(Component.text("§6${currentTab.displayName} 도감 진행도"))
                lore(
                    listOf(
                        Component.text("§7발견 : $discoveredCount/$totalCount"),
                        Component.text(
                            "§7달성률 : ${
                                String.format(
                                    "%.0f",
                                    discoveredCount.toDouble() / totalCount * 100
                                )
                            }%"
                        )
                    )
                )
            }
        }

        pane.addItem(GuiItem(statsItem))
        pane.setRepeat(true)

        return pane
    }

    private fun createTabPane(): Pane {
        val pattern = Pattern("00", "11", "22", "33", "44", "55")
        val pane = PatternPane(7, 0, 2, 6, pattern)

        ItemTab.values().forEachIndexed { index, tab ->
            val tabItem = ItemStack(tab.glassColor).apply {
                itemMeta = itemMeta?.apply {
                    setCustomModelData(17741)
                    displayName(Component.text("§f${tab.displayName}"))
                }
            }

            pane.bindItem(index.toString().first(), GuiItem(tabItem) { event ->
                event.isCancelled = true
                currentTab = tab

                panes.remove(itemsPane)
                itemsPane = createItemsPane()
                addPane(itemsPane!!)

                panes.remove(controlPane)
                controlPane = createControlPane()
                addPane(controlPane!!)

                panes.remove(statsPane)
                statsPane = createStatsPane()
                addPane(statsPane!!)

                update()
            })
        }

        return pane
    }

    private enum class PageController
        (
        private val shouldContinue: BiPredicate<Int, PaginatedPane?>,
        private val nextPageSupplier: IntUnaryOperator,
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
            val item = ItemStack(Material.PAPER)
            val meta = item.itemMeta
            meta.setCustomModelData(17741)
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

    private enum class ItemTab(val displayName: String, val glassColor: Material, val itemFilter: Predicate<Material>) {
        ALL("전체", Material.PAPER, Predicate { true }),
        BLOCKS("블록", Material.PAPER, Predicate { it.isBlock && !it.isAir }),
        TOOLS(
            "도구",
            Material.PAPER,
            Predicate {
                it.name.endsWith("_PICKAXE") || it.name.endsWith("_AXE") || it.name.endsWith("_SHOVEL") || it.name.endsWith(
                    "_HOE"
                ) || it.name.endsWith("_SWORD")
            }),
        COMBAT(
            "전투",
            Material.PAPER,
            Predicate {
                it.name.endsWith("_SWORD") || it.name.endsWith("_HELMET") || it.name.endsWith("_CHESTPLATE") || it.name.endsWith(
                    "_LEGGINGS"
                ) || it.name.endsWith("_BOOTS") || it == Material.BOW || it == Material.ARROW
            }),
        FOOD("음식", Material.PAPER, Predicate { it.isEdible }),
        REDSTONE(
            "레드스톤",
            Material.PAPER,
            Predicate {
                it.name.contains("REDSTONE") || it.name.contains("REPEATER") || it.name.contains("COMPARATOR") || it.name.contains(
                    "PISTON"
                )
            })
    }

    class Builder(var title: String) {
        var itemTransformer: Function<Material, GuiItem>? = null
        var itemFilter: Predicate<Material>? = null
        lateinit var player: Player
        lateinit var collectionManager: ItemCollectionManager

        fun asItem(itemFilter: Predicate<Material>) = apply { this.itemFilter = itemFilter }
        fun forPlayer(player: Player) = apply { this.player = player }
        fun withCollectionManager(manager: ItemCollectionManager) = apply { this.collectionManager = manager }
        fun build() = EncyclopediaGUI(this)
    }

    companion object {
        private const val ITEMS_PER_PAGE = 5 * 5
    }
}