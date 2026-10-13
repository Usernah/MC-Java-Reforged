package net.jr.compat.curios.screen;

import com.mojang.datafixers.util.Pair;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.client.ui.container.slots.ItemSlotMaps;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.screens.InventoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import top.theillusivec4.curios.CuriosConstants;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICuriosScreen;
import top.theillusivec4.curios.client.screen.CuriosScreen;
import top.theillusivec4.curios.client.screen.button.CosmeticButton;
import top.theillusivec4.curios.client.screen.button.CuriosButton;
import top.theillusivec4.curios.client.screen.button.RenderButton;
import top.theillusivec4.curios.common.inventory.CurioSlot;
import top.theillusivec4.curios.common.inventory.container.CuriosMenu;
import top.theillusivec4.curios.common.network.client.CPacketOpenVanilla;
import top.theillusivec4.curios.common.network.client.CPacketPage;
import top.theillusivec4.curios.common.network.client.CPacketToggleCosmetics;
import top.theillusivec4.curios.common.network.client.CPacketToggleRender;
import top.theillusivec4.curios.config.CuriosClientConfig;

import java.util.ArrayList;
import java.util.List;

public final class CuriosInventoryScreen extends AbstractContainerScreen<CuriosMenu>
    implements CustomItemSlotLayout, ICuriosScreen {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/container/inventory.png");
    private static final Identifier CURIO_INVENTORY = Identifier.fromNamespaceAndPath(
        CuriosConstants.MOD_ID,
        "textures/gui/curios/inventory.png"
    );

    private final List<RenderButton> renderButtons = new ArrayList<>();
    private VanillaItemSlotLayer itemSlotLayer;
    private ItemSlotGrid itemSlotGrid;
    private Slot lastSlot;
    private int slotCount = -1;
    private int currentPage = -1;
    private boolean viewingCosmetics;

    public CuriosInventoryScreen(CuriosMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.itemSlotLayer = new VanillaItemSlotLayer(this, this.menu, this.font, 18);
        this.rebuildCuriosLayout();
        this.addCuriosButtons();
    }

    private void rebuildCuriosLayout() {
        ItemSlotGrid grid = new ItemSlotGrid(ItemSlotMaps.SURVIVAL_INVENTORY)
            .grid(ItemSlotGroup.PLAYER_ARMOR, this.leftPos + 46, this.topPos + 7, 1, 18, 18, 16)
            .point(ItemSlotGroup.PLAYER_OFFHAND, 0, this.leftPos + 115, this.topPos + 61, 18, 18, 16)
            .grid(ItemSlotGroup.PLAYER_INVENTORY, this.leftPos + 7, this.topPos + 83, 9, 18, 18, 16)
            .grid(ItemSlotGroup.PLAYER_HOTBAR, this.leftPos + 7, this.topPos + 141, 9, 18, 18, 16);

        for (int slotId = 46; slotId < this.menu.slots.size(); slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            grid.slot(
                slot.index,
                this.leftPos + slot.x - 1,
                this.topPos + slot.y - 1,
                18,
                18,
                16
            );
        }

        this.itemSlotGrid = grid;
        this.slotCount = this.menu.slots.size();
        this.lastSlot = this.menu.slots.isEmpty() ? null : this.menu.slots.getLast();
        this.currentPage = this.menu.currentPage;
        this.viewingCosmetics = this.menu.isViewingCosmetics;
    }

    private void addCuriosButtons() {
        Pair<Integer, Integer> offsets = CuriosScreen.getButtonOffset(false);
        if (CuriosClientConfig.CLIENT.enableButton.get()) {
            this.addRenderableWidget(new ImageButton(
                this.leftPos + offsets.getFirst() + 2,
                this.topPos + offsets.getSecond() + 85,
                10,
                10,
                CuriosButton.BIG,
                button -> this.returnToInventory()
            ));
        }

        if (this.menu.hasCosmetics) {
            WidgetSprites sprites = this.menu.isViewingCosmetics ? CosmeticButton.ON : CosmeticButton.OFF;
            ImageButton cosmetics = new ImageButton(
                this.leftPos - 27,
                this.topPos - 18,
                20,
                17,
                sprites,
                button -> {
                    this.menu.toggleCosmetics();
                    ClientPacketDistributor.sendToServer(new CPacketToggleCosmetics(this.menu.containerId));
                    this.rebuildWidgets();
                }
            );
            cosmetics.setTooltip(Tooltip.create(Component.translatable("gui.curios.toggle.cosmetics")));
            this.addRenderableWidget(cosmetics);
        }

        if (this.menu.totalPages > 1) {
            this.addRenderableWidget(this.createPageButton(false));
            this.addRenderableWidget(this.createPageButton(true));
        }

        this.renderButtons.clear();
        for (int slotId = 46; slotId < this.menu.slots.size(); slotId++) {
            Slot slot = this.menu.slots.get(slotId);
            if (slot instanceof CurioSlot curioSlot && curioSlot.canToggleRender()) {
                var pos = this.itemSlotGrid.get(slot.index);
                RenderButton renderButton = new RenderButton(
                    curioSlot,
                    pos.x() + 13,
                    pos.y(),
                    8,
                    8,
                    75,
                    0,
                    CURIO_INVENTORY,
                    button -> ClientPacketDistributor.sendToServer(
                        new CPacketToggleRender(curioSlot.getId(), slot.getSlotIndex())
                    )
                );
                this.renderButtons.add(renderButton);
                this.addRenderableWidget(renderButton);
            }
        }
    }

    private Button createPageButton(boolean next) {
        return new Button(
            this.leftPos + (next ? -17 : -28),
            this.topPos + 2,
            11,
            12,
            CommonComponents.EMPTY,
            button -> ClientPacketDistributor.sendToServer(new CPacketPage(this.menu.containerId, next)),
            narration -> narration.get()
        ) {
            @Override
            public void extractContents(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float partialTick
            ) {
                this.active = next
                    ? CuriosInventoryScreen.this.menu.currentPage + 1 < CuriosInventoryScreen.this.menu.totalPages
                    : CuriosInventoryScreen.this.menu.currentPage > 0;
                int textureX = next ? 43 : 32;
                int textureY = 25;
                if (!this.active) {
                    textureY += 12;
                } else if (this.isHoveredOrFocused()) {
                    textureX += 22;
                }
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    CURIO_INVENTORY,
                    this.getX(),
                    this.getY(),
                    textureX,
                    textureY,
                    this.width,
                    this.height,
                    256,
                    256
                );
            }
        };
    }

    private void returnToInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ItemStack carried = this.menu.getCarried();
        this.menu.setCarried(ItemStack.EMPTY);
        minecraft.player.clientSideCloseContainer();
        minecraft.gui.setScreen(new InventoryScreen(
            minecraft.player.inventoryMenu,
            minecraft.player.getInventory(),
            Component.translatable("container.inventory")
        ));
        minecraft.player.inventoryMenu.setCarried(carried);
        ClientPacketDistributor.sendToServer(new CPacketOpenVanilla(carried));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int count = this.menu.slots.size();
        Slot newestLastSlot = count == 0 ? null : this.menu.slots.getLast();
        if (count != this.slotCount
            || newestLastSlot != this.lastSlot
            || this.menu.currentPage != this.currentPage
            || this.menu.isViewingCosmetics != this.viewingCosmetics) {
            this.rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        Draw.imageFromMeta(BACKGROUND, this.leftPos, this.topPos, this.imageWidth, this.imageHeight)
            .draw(graphics);

        if (this.minecraft != null && this.minecraft.player != null) {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                this.leftPos + 64,
                this.topPos + 7,
                this.leftPos + 113,
                this.topPos + 77,
                30,
                0.0625F,
                mouseX,
                mouseY,
                this.minecraft.player
            );
            this.extractCuriosPanel(graphics);
        }
    }

    private void extractCuriosPanel(GuiGraphicsExtractor graphics) {
        CuriosApi.getCuriosInventory(this.minecraft.player).ifPresent(handler -> {
            int xOffset = -33;
            int yOffset = this.topPos;
            boolean pageOffset = this.menu.totalPages > 1;

            if (this.menu.hasCosmetics) {
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    CURIO_INVENTORY,
                    this.leftPos + xOffset + 2,
                    yOffset - 23,
                    32,
                    0,
                    28,
                    24,
                    256,
                    256
                );
            }

            List<Integer> grid = this.menu.grid;
            xOffset -= (grid.size() - 1) * 18;
            for (int column = 0; column < grid.size(); column++) {
                int rows = grid.getFirst();
                int upperHeight = 7 + rows * 18 + (pageOffset ? 8 : 0);
                int textureX = 91 + (column == 0 ? 0 : 7);
                graphics.blit(RenderPipelines.GUI_TEXTURED, CURIO_INVENTORY, this.leftPos + xOffset, yOffset,
                    textureX, 0, 25, upperHeight, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, CURIO_INVENTORY, this.leftPos + xOffset,
                    yOffset + upperHeight, textureX, 159, 25, 7, 256, 256);
                if (grid.size() == 1) {
                    textureX += 7;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, CURIO_INVENTORY, this.leftPos + xOffset + 7,
                        yOffset, textureX, 0, 25, upperHeight, 256, 256);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, CURIO_INVENTORY, this.leftPos + xOffset + 7,
                        yOffset + upperHeight, textureX, 159, 25, 7, 256, 256);
                }
                xOffset += column == 0 ? 25 : 18;
            }

            xOffset -= grid.size() * 18;
            if (pageOffset) {
                yOffset += 8;
            }
            for (int rows : grid) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, CURIO_INVENTORY, this.leftPos + xOffset,
                    yOffset + 7, 7, 7, 18, rows * 18, 256, 256);
                xOffset += 18;
            }

            for (Slot slot : this.menu.slots) {
                if (slot instanceof CurioSlot curioSlot && curioSlot.isCosmetic()) {
                    var pos = this.itemSlotGrid.get(slot.index);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, CURIO_INVENTORY,
                        pos.x(), pos.y(), 32, 50, 18, 18, 256, 256);
                }
            }
        });
    }

    @Override
    public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        this.itemSlotLayer.updateHovered(this.itemSlotGrid, mouseX, mouseY);
        this.itemSlotLayer.renderItemSlotMap(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderHoveredItemSlot(graphics, this.itemSlotGrid);
        for (RenderButton renderButton : this.renderButtons) {
            renderButton.renderButtonOverlay(graphics, mouseX, mouseY, partialTick);
        }
        this.itemSlotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.extractSlotTooltip(graphics, mouseX, mouseY);
    }

    private void extractSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot slot = this.itemSlotLayer.getHoveredItemSlot();
        if (slot == null || !this.menu.getCarried().isEmpty()) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (slot instanceof CurioSlot curioSlot) {
            stack = curioSlot.getSlotExtension().getDisplayStack(curioSlot.getSlotContext(), stack);
            if (stack.isEmpty()) {
                graphics.setTooltipForNextFrame(
                    this.font,
                    curioSlot.getSlotTooltip(),
                    stack.getTooltipImage(),
                    mouseX,
                    mouseY
                );
                return;
            }
        }
        if (stack.isEmpty()) {
            return;
        }
        List<Component> tooltip = Screen.getTooltipFromItem(this.minecraft, stack);
        if (slot instanceof CurioSlot curioSlot && !curioSlot.isActiveState()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("curios.tooltip.inactive").withStyle(ChatFormatting.RED));
        }
        graphics.setTooltipForNextFrame(this.font, tooltip, stack.getTooltipImage(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        GuiEventListener child = this.getChildAt(event.x(), event.y()).orElse(null);
        if (child != null) {
            boolean handled = child.mouseClicked(event, doubleClick);
            if (handled && child.shouldTakeFocusAfterInteraction()) {
                this.setFocused(child);
                if (event.button() == 0) {
                    this.setDragging(true);
                }
            }
            return true;
        }
        return this.itemSlotLayer.mouseClicked(
            this.itemSlotGrid,
            event.x(),
            event.y(),
            event.button(),
            true,
            this.leftPos - this.menu.panelWidth,
            this.topPos,
            this.imageWidth + this.menu.panelWidth,
            this.imageHeight
        );
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            GuiEventListener focused = this.getFocused();
            if (focused != null) {
                return focused.mouseReleased(event);
            }
        }
        return this.itemSlotLayer.mouseReleased(
            this.itemSlotGrid,
            event.x(),
            event.y(),
            event.button(),
            true,
            this.leftPos - this.menu.panelWidth,
            this.topPos,
            this.imageWidth + this.menu.panelWidth,
            this.imageHeight
        );
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener focused = this.getFocused();
        if (focused != null && this.isDragging() && event.button() == 0) {
            return focused.mouseDragged(event, dragX, dragY);
        }
        return this.itemSlotLayer.mouseDragged(
            this.itemSlotGrid,
            event.x(),
            event.y(),
            event.button(),
            dragX,
            dragY
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.menu.totalPages > 1
            && mouseX < this.leftPos
            && mouseX > this.leftPos - this.menu.panelWidth
            && mouseY > this.topPos
            && mouseY < this.topPos + this.imageHeight) {
            ClientPacketDistributor.sendToServer(new CPacketPage(this.menu.containerId, vertical < 0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.itemSlotLayer.keyPressed(this.itemSlotGrid, event.key(), event.scancode())
            || super.keyPressed(event);
    }

    @Override
    public ItemSlotGrid itemSlotGrid() {
        return this.itemSlotGrid;
    }

    @Override
    public VanillaItemSlotLayer itemSlotLayer() {
        return this.itemSlotLayer;
    }
}
