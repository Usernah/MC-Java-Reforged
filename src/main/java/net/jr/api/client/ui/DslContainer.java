package net.jr.api.client.ui;

import net.jr.client.ui.container.DslContainerBridge;
import net.jr.client.ui.container.DslContainerContext;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.dsl.DslNode;
import net.jr.client.ui.dsl.UiDataContext;
import net.jr.client.ui.dsl.UiRenderLayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DslContainer extends FramebufferScreen {
    UiContainerFile dslFile();

    default DslContainerContext dslContext() {
        if (!(this instanceof AbstractContainerScreen<?> container)) {
            throw new IllegalStateException(
                "DslContainer must be implemented by a Minecraft AbstractContainerScreen"
            );
        }
        return ((DslContainerBridge)container).javaReforged$dslContainerContext(this.dslFile());
    }

    default DslNode dsl(String id) {
        return this.dslContext().element(id);
    }

    default UiDataContext dslData() {
        return this.dslContext().data();
    }

    default AbstractContainerMenu dslMenu() {
        return this.dslContext().menu();
    }

    default List<Slot> dslItemSlots() {
        return this.dslContext().itemSlots();
    }

    @Nullable
    default ItemSlotGrid dslItemSlotGrid(String id) {
        DslNode node = this.dsl(id);
        if (!node.isPresent()) {
            return null;
        }
        ItemSlotGrid grid = node.recover(null, () -> new ItemSlotGrid(node));
        return node.isUsable() ? grid : null;
    }

    default void extractDslBackground(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        this.dslContext().extract(UiRenderLayer.BACKGROUND, graphics, mouseX, mouseY, partialTick);
    }

    default void extractDslContent(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        this.dslContext().extract(UiRenderLayer.CONTENT, graphics, mouseX, mouseY, partialTick);
    }

    default void extractDslForeground(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        this.dslContext().extract(UiRenderLayer.FOREGROUND, graphics, mouseX, mouseY, partialTick);
    }
}
