package net.jr.client.ui.container.slots;

import net.jr.client.ui.UiElement;
import net.jr.client.ui.container.DslContainerContext;
import net.jr.client.ui.dsl.DslElement;
import net.jr.client.ui.dsl.DslNode;
import net.jr.client.ui.dsl.UiCompiledDocument;
import net.jr.client.ui.dsl.UiCompiledDocument.EntryDefinition;
import net.jr.client.ui.dsl.UiCompiledDocument.RenderableDefinition;
import net.jr.client.ui.dsl.UiDslKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ItemSlotGrid implements DslElement {
    public static final UiDslKind<ItemSlotGrid> DSL = new UiDslKind<>("item_slots", ItemSlotGrid.class);

    private final ItemSlotMap map;
    private final Map<Integer, ItemSlotPos> positions = new TreeMap<>();
    private final int originX;
    private final int originY;
    private final DslNode dsl;
    private final VanillaItemSlotLayer itemSlotLayer;
    private final Map<Integer, List<UiElement.ElementRenderable>> backgrounds = new LinkedHashMap<>();
    private final Map<List<EntryDefinition>, List<UiElement.ElementRenderable>> backgroundCache =
        new IdentityHashMap<>();

    public ItemSlotGrid(ItemSlotMap map) {
        this.map = map;
        this.originX = 0;
        this.originY = 0;
        this.dsl = null;
        this.itemSlotLayer = null;
    }

    /** Builds slot layout and per-slot visuals from an item_slots DSL declaration. */
    public ItemSlotGrid(DslNode dsl) {
        if (!(dsl.context() instanceof DslContainerContext containerContext)) {
            throw new IllegalArgumentException("item_slots can only be used by a DslContainer");
        }
        this.dsl = dsl;
        this.map = ItemSlotMaps.named(dsl.text("map"));
        this.originX = Math.round(containerContext.originX());
        this.originY = Math.round(containerContext.originY());
        this.itemSlotLayer = new VanillaItemSlotLayer(
            containerContext.container(),
            containerContext.menu(),
            Minecraft.getInstance().font,
            18
        );

        int defaultItemSize = dsl.integerOr("item_size", 16);
        requirePositive("item_size", defaultItemSize);
        List<EntryDefinition> defaultBackground = dsl.block("background");
        Map<Integer, List<EntryDefinition>> slotBackgrounds = new LinkedHashMap<>();
        this.configureLayouts(dsl, defaultItemSize, slotBackgrounds);
        for (int slotId : this.positions.keySet()) {
            if (slotId < 0 || slotId >= containerContext.menu().slots.size()) {
                throw new IllegalArgumentException(
                    "Item slot layout references menu slot " + slotId + " but the menu has only "
                        + containerContext.menu().slots.size() + " slots"
                );
            }
        }
        for (int slotId : this.positions.keySet()) {
            List<EntryDefinition> definitions = slotBackgrounds.getOrDefault(slotId, defaultBackground);
            this.backgrounds.put(slotId, this.backgroundCache.computeIfAbsent(
                definitions,
                ItemSlotGrid::createBackgroundRenderables
            ));
        }
        dsl.finish(DSL, this);
    }

    private static List<UiElement.ElementRenderable> createBackgroundRenderables(
        List<EntryDefinition> definitions
    ) {
        List<RenderableDefinition> renderables = definitions.stream()
            .map(definition -> {
                if (definition instanceof RenderableDefinition renderable) {
                    return renderable;
                }
                throw new IllegalArgumentException("item_slots background only accepts visual elements");
            })
            .toList();
        return UiElement.renderables(renderables);
    }

    private void configureLayouts(
        DslNode declaration,
        int defaultItemSize,
        Map<Integer, List<EntryDefinition>> slotBackgrounds
    ) {
        for (DslNode layout : declaration.children()) {
            this.configureLayout(layout, defaultItemSize, slotBackgrounds);
        }
    }

    private void configureLayout(
        DslNode layout,
        int defaultItemSize,
        Map<Integer, List<EntryDefinition>> slotBackgrounds
    ) {
        String type = layout.type();
        if (!(type.equals("grid") || type.equals("line") || type.equals("column")
            || type.equals("point") || type.equals("slot"))) {
            throw new IllegalArgumentException("Unknown item slot layout '" + type + "'");
        }
        ItemSlotGroup group = null;
        if (!type.equals("slot")) {
            group = parseGroup(layout.text("group"));
            if (!this.map.has(group)) {
                throw new IllegalArgumentException("Item slot map has no group '" + group + "'");
            }
        }
        int x = layout.integer("x") + this.originX;
        int y = layout.integer("y") + this.originY;
        int itemSize = layout.integerOr("item_size", defaultItemSize);
        int width = dimension(layout, "width", itemSize + 2);
        int height = dimension(layout, "height", itemSize + 2);
        requirePositive("item_size", itemSize);
        requirePositive("width", width);
        requirePositive("height", height);
        int firstSlot;
        switch (type) {
            case "grid" -> {
                int columns = layout.integer("columns");
                requirePositive("columns", columns);
                ItemSlotSlice slice = this.map.get(group);
                firstSlot = slice.slot(0);
                this.grid(group, x, y, columns, width, height, itemSize);
            }
            case "line" -> {
                ItemSlotSlice slice = this.map.get(group);
                firstSlot = slice.slot(0);
                this.line(group, x, y, width, height, itemSize);
            }
            case "column" -> {
                ItemSlotSlice slice = this.map.get(group);
                firstSlot = slice.slot(0);
                this.column(group, x, y, width, height, itemSize);
            }
            case "point" -> {
                int index = layout.integer("index");
                firstSlot = this.map.slot(group, index);
                this.point(group, index, x, y, width, height, itemSize);
            }
            case "slot" -> {
                firstSlot = layout.integer("index");
                this.slot(firstSlot, x, y, width, height, itemSize);
            }
            default -> throw new IllegalArgumentException("Unknown item slot layout '" + type + "'");
        }

        boolean hasBackground = layout.hasBlock("background");
        List<EntryDefinition> background = layout.block("background");
        if (hasBackground) {
            int slotCount = type.equals("slot") || type.equals("point") ? 1 : this.map.get(group).size();
            for (int index = 0; index < slotCount; index++) {
                int slotId = type.equals("slot") || type.equals("point")
                    ? firstSlot
                    : this.map.get(group).slot(index);
                slotBackgrounds.put(slotId, background);
            }
        }
        layout.finishBlock();
    }

    private static int dimension(DslNode node, String name, int fallback) {
        return node.hasProperty(name) ? node.integer(name) : node.integerOr("size", fallback);
    }

    private static void requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Item slot '" + name + "' must be greater than zero");
        }
    }

    private static ItemSlotGroup parseGroup(String name) {
        try {
            return ItemSlotGroup.valueOf(name.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown item slot group '" + name + "'", exception);
        }
    }

    public ItemSlotGrid grid(ItemSlotGroup group, int startX, int startY, int columns, int size) {
        return this.grid(group, startX, startY, columns, size, size);
    }

    public ItemSlotGrid grid(
        ItemSlotGroup group,
        int startX,
        int startY,
        int columns,
        int width,
        int height
    ) {
        return this.grid(group, startX, startY, columns, width, height,
            Math.max(1, Math.min(16, Math.min(width, height) - 2)));
    }

    public ItemSlotGrid grid(
        ItemSlotGroup group,
        int startX,
        int startY,
        int columns,
        int width,
        int height,
        int itemSize
    ) {
        ItemSlotSlice slice = map.get(group);

        for (int i = 0; i < slice.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            positions.put(
                slice.slot(i),
                new ItemSlotPos(startX + col * width, startY + row * height, width, height, itemSize)
            );
        }

        return this;
    }

    public ItemSlotGrid line(ItemSlotGroup group, int startX, int startY, int size) {
        return this.line(group, startX, startY, size, size);
    }

    public ItemSlotGrid line(ItemSlotGroup group, int startX, int startY, int width, int height) {
        return this.line(
            group,
            startX,
            startY,
            width,
            height,
            Math.max(1, Math.min(16, Math.min(width, height) - 2))
        );
    }

    public ItemSlotGrid line(
        ItemSlotGroup group,
        int startX,
        int startY,
        int width,
        int height,
        int itemSize
    ) {
        ItemSlotSlice slice = map.get(group);

        for (int i = 0; i < slice.size(); i++) {
            positions.put(
                slice.slot(i),
                new ItemSlotPos(startX + i * width, startY, width, height, itemSize)
            );
        }

        return this;
    }

    public ItemSlotGrid column(ItemSlotGroup group, int startX, int startY, int size) {
        return this.column(group, startX, startY, size, size);
    }

    public ItemSlotGrid column(ItemSlotGroup group, int startX, int startY, int width, int height) {
        return this.column(
            group,
            startX,
            startY,
            width,
            height,
            Math.max(1, Math.min(16, Math.min(width, height) - 2))
        );
    }

    public ItemSlotGrid column(
        ItemSlotGroup group,
        int startX,
        int startY,
        int width,
        int height,
        int itemSize
    ) {
        ItemSlotSlice slice = map.get(group);

        for (int i = 0; i < slice.size(); i++) {
            positions.put(
                slice.slot(i),
                new ItemSlotPos(startX, startY + i * height, width, height, itemSize)
            );
        }

        return this;
    }

    public ItemSlotGrid point(ItemSlotGroup group, int localIndex, int x, int y) {
        return this.point(group, localIndex, x, y, 16, 16);
    }

    public ItemSlotGrid point(ItemSlotGroup group, int localIndex, int x, int y, int width, int height) {
        return this.point(
            group,
            localIndex,
            x,
            y,
            width,
            height,
            Math.max(1, Math.min(16, Math.min(width, height) - 2))
        );
    }

    public ItemSlotGrid point(
        ItemSlotGroup group,
        int localIndex,
        int x,
        int y,
        int width,
        int height,
        int itemSize
    ) {
        positions.put(map.slot(group, localIndex), new ItemSlotPos(x, y, width, height, itemSize));
        return this;
    }

    public ItemSlotGrid slot(int slotId, int x, int y, int width, int height, int itemSize) {
        positions.put(slotId, new ItemSlotPos(x, y, width, height, itemSize));
        return this;
    }

    public ItemSlotPos get(int slotId) {
        return positions.get(slotId);
    }

    public ItemSlotPos get(ItemSlotGroup group, int localIndex) {
        return get(map.slot(group, localIndex));
    }

    public boolean has(int slotId) {
        return positions.containsKey(slotId);
    }

    public Set<Integer> slotIds() {
        return Set.copyOf(this.positions.keySet());
    }

    public VanillaItemSlotLayer itemSlotLayer() {
        if (this.itemSlotLayer == null) {
            throw new IllegalStateException("This ItemSlotGrid was not created from a container DSL");
        }
        return this.itemSlotLayer;
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        if (this.dsl == null || this.itemSlotLayer == null) {
            throw new IllegalStateException("Only DSL item_slots declarations can be extracted");
        }
        this.itemSlotLayer.updateHovered(this, mouseX, mouseY);
        this.renderSlotBackgrounds(graphics);
        this.itemSlotLayer.renderItemSlotMap(graphics, this);
        this.itemSlotLayer.renderHoveredItemSlot(graphics, this);
        this.itemSlotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.itemSlotLayer.renderTooltip(graphics, this, mouseX, mouseY);
    }

    private void renderSlotBackgrounds(GuiGraphicsExtractor graphics) {
        for (Map.Entry<Integer, List<UiElement.ElementRenderable>> entry : this.backgrounds.entrySet()) {
            ItemSlotPos position = this.get(entry.getKey());
            if (position == null) {
                continue;
            }
            Slot slot = this.itemSlotLayer.getItemSlotAt(this, position.x() + position.width() / 2.0,
                position.y() + position.height() / 2.0);
            UiCompiledDocument.DataContext scope = path -> {
                if (!path.isEmpty() && path.getFirst().equals("this")) {
                    if (path.size() != 2) {
                        throw new IllegalArgumentException("Unknown item slot DSL value: ${" + String.join(".", path) + "}");
                    }
                    return switch (path.get(1)) {
                        case "x" -> position.x();
                        case "y" -> position.y();
                        case "width", "w" -> position.width();
                        case "height", "h" -> position.height();
                        case "item_size" -> position.itemSize();
                        case "index" -> entry.getKey();
                        case "group" -> {
                            ItemSlotGroup group = this.map.findGroup(entry.getKey());
                            yield group == null ? "custom" : group.name().toLowerCase(java.util.Locale.ROOT);
                        }
                        case "hovered" -> slot != null && this.itemSlotLayer.getHoveredItemSlot() == slot;
                        case "focused" -> slot != null && this.itemSlotLayer.getFocusedItemSlot() == slot;
                        case "empty" -> slot == null || !slot.hasItem();
                        default -> throw new IllegalArgumentException(
                            "Unknown item slot DSL value: ${this." + path.get(1) + "}"
                        );
                    };
                }
                return this.dsl.data().resolve(path);
            };
            for (UiElement.ElementRenderable background : entry.getValue()) {
                background.render(graphics, scope, position.x(), position.y());
            }
        }
    }

    @Override
    public void closeDsl() {
        for (List<UiElement.ElementRenderable> renderables : this.backgroundCache.values()) {
            for (UiElement.ElementRenderable renderable : renderables) {
                renderable.close();
            }
        }
        this.backgroundCache.clear();
        this.backgrounds.clear();
    }
}

