package net.jr.client.ui.container;

import net.jr.api.client.ui.UiContainerFile;
import net.jr.client.ui.dsl.DslUiContext;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;


public final class DslContainerContext extends DslUiContext<UiContainerFile> {
    private final AbstractContainerScreen<?> container;

    public DslContainerContext(AbstractContainerScreen<?> container, UiContainerFile file) {
        super(container, file);
        this.container = container;
    }

    public AbstractContainerScreen<?> container() {
        return this.container;
    }

    public AbstractContainerMenu menu() {
        return this.container.getMenu();
    }

    public List<Slot> itemSlots() {
        return List.copyOf(this.menu().slots);
    }

    @Override
    public float originX() {
        return this.access().javaReforged$getLeftPos();
    }

    @Override
    public float originY() {
        return this.access().javaReforged$getTopPos();
    }

    public int width() {
        return this.access().javaReforged$getImageWidth();
    }

    public int height() {
        return this.access().javaReforged$getImageHeight();
    }

    public void applyBoundsOffset() {
        if (!this.hasBounds()) {
            return;
        }
        AbstractContainerScreenAccessor access = this.access();
        access.javaReforged$setLeftPos(access.javaReforged$getLeftPos() + this.boundsX());
        access.javaReforged$setTopPos(access.javaReforged$getTopPos() + this.boundsY());
    }

    public boolean supportsBlock(String blockId) {
        if (!this.hasProperty("compatible_blocks")) {
            return false;
        }
        Object value = this.property("compatible_blocks");
        if (!(value instanceof List<?> blocks)) {
            this.report(
                "$document", "container compatible_blocks must be a list", null
            );
            return false;
        }
        return blocks.stream().anyMatch(blockId::equals);
    }

    private AbstractContainerScreenAccessor access() {
        return (AbstractContainerScreenAccessor)this.container;
    }

}
