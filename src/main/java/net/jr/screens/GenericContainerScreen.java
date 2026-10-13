package net.jr.screens;

import net.jr.api.client.ui.DslContainer;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.client.ui.container.DslContainerSize;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.registry.ModUi;
import net.jr.client.ui.dsl.UiDocumentManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.jetbrains.annotations.Nullable;

/** Visual host for vanilla generic 9-column menus. */
public final class GenericContainerScreen extends AbstractContainerScreen<ChestMenu>
    implements DslContainer, CustomItemSlotLayout {
    private final UiContainerFile dslFile;
    @Nullable
    private Identifier blockId;
    @Nullable
    private ItemSlotGrid dslMenu;

    public GenericContainerScreen(ChestMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, definition(menu));
    }

    private GenericContainerScreen(
        ChestMenu menu,
        Inventory inventory,
        Component title,
        Definition definition
    ) {
        super(
            menu,
            inventory,
            title,
            definition.dimensions().width(),
            definition.dimensions().height()
        );
        this.dslFile = definition.file();
    }

    private static Definition definition(ChestMenu menu) {
        UiContainerFile file = dslFileFor(menu);
        if (file == null) {
            throw new IllegalArgumentException(
                "Unsupported generic container row count: " + menu.getRowCount()
            );
        }
        int vanillaHeight = 114 + menu.getRowCount() * 18;
        DslContainerSize dimensions = DslContainerSize.resolve(file, 176, vanillaHeight);
        return new Definition(file, dimensions);
    }

    @Nullable
    public static UiContainerFile dslFileFor(ChestMenu menu) {
        return switch (menu.getRowCount()) {
            case 3 -> ModUi.GENERIC_27;
            case 6 -> ModUi.GENERIC_54;
            default -> null;
        };
    }

    public static boolean hasCompiledDsl(ChestMenu menu) {
        UiContainerFile file = dslFileFor(menu);
        return file != null && UiDocumentManager.getInstance().getCompiled(file).isPresent();
    }

    @Override
    public UiContainerFile dslFile() {
        return this.dslFile;
    }

    @Override
    protected void init() {
        // AbstractContainerScreen.init closes its DSL context. The binding belongs to
        // that context, so it must be recreated when this screen instance is resized.
        this.dslMenu = null;
        super.init();
        this.dslData().exposeText("container.block_id", () ->
            this.blockId == null ? "" : this.blockId.toString()
        );
        this.dslData().exposeBoolean("container.has_block_origin", () -> this.blockId != null);
        this.dslData().exposeNumber("container.rows", () -> this.menu.getRowCount());
        this.dslData().exposeText("container.title", () -> this.title.getString());
        this.dslData().exposeText(
            "container.inventory_title",
            () -> this.playerInventoryTitle.getString()
        );
        this.bindDslMenuIfSupported();
    }

    public void setBlockOrigin(int containerId, String blockId) {
        if (this.menu.containerId != containerId) {
            return;
        }
        this.blockId = blockId.isEmpty() ? null : Identifier.tryParse(blockId);
        this.bindDslMenuIfSupported();
    }

    private boolean supportsDslLayout() {
        return this.blockId != null
            && this.dslContext().supportsBlock(this.blockId.toString());
    }

    private void bindDslMenuIfSupported() {
        if (this.dslMenu == null && this.supportsDslLayout()) {
            this.dslMenu = this.dslItemSlotGrid("menu");
        }
    }

    @Override
    public boolean isItemSlotLayoutActive() {
        return this.dslMenu != null && this.supportsDslLayout();
    }

    @Override
    public ItemSlotGrid itemSlotGrid() {
        if (this.dslMenu == null) {
            throw new IllegalStateException("The generic container DSL item slots are not bound");
        }
        return this.dslMenu;
    }

    @Override
    public VanillaItemSlotLayer itemSlotLayer() {
        return this.itemSlotGrid().itemSlotLayer();
    }

    @Override
    public void extractBackground(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (this.supportsDslLayout()) {
            this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        if (!this.supportsDslLayout()) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        this.bindDslMenuIfSupported();
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.extractDslForeground(graphics, mouseX, mouseY, partialTick);
    }

    private record Definition(UiContainerFile file, DslContainerSize dimensions) {
    }
}
