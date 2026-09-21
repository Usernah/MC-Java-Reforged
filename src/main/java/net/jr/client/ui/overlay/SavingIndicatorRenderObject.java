package net.jr.client.ui.overlay;

import com.mojang.blaze3d.textures.FilterMode;
import net.jr.api.client.render.gui.GlobalRenderContext;
import net.jr.api.client.render.gui.IGlobalRenderObject;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.ImageElement;
import net.jr.client.ui.render.GuiCompositionLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/** A single, window-wide autosave notice backed by a 1920x1080 composition. */
public final class SavingIndicatorRenderObject implements IGlobalRenderObject, AutoCloseable {
    private static final int CANVAS_WIDTH = 1920;
    private static final int CANVAS_HEIGHT = 1080;
    private static final int ICON_WIDTH = 44 * 2;
    private static final int ICON_HEIGHT = 76 * 2;
    private static final int MARGIN = 10;
    private static final long VISIBLE_MS = 3_000L;
    private static final float PIXEL_BIAS = 0.35F;
    private static final Asset ICON = Asset.MOD("textures/gui/animated/save_icon.png");

    private final GuiCompositionLayer layer = new GuiCompositionLayer(CANVAS_WIDTH, CANVAS_HEIGHT, 1.0F);
    private final ImageElement image = new ImageElement(ICON, 0, 0, ICON_WIDTH, ICON_HEIGHT, null);
    private boolean wasSaving;
    private long visibleUntilMs;

    public SavingIndicatorRenderObject() {
        this.image.setUseMeta(true);
    }

    @Override
    public void render(GlobalRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server == null || !minecraft.options.showAutosaveIndicator().get()) {
            this.wasSaving = false;
            this.visibleUntilMs = 0L;
            return;
        }

        long now = Util.getMillis();
        boolean saving = server.isCurrentlySaving();
        if (saving && !this.wasSaving) {
            this.visibleUntilMs = now + VISIBLE_MS;
        }
        this.wasSaving = saving;
        if (now >= this.visibleUntilMs) {
            return;
        }

        this.layer.submitTopRight(context.graphics(), FilterMode.LINEAR, PIXEL_BIAS, canvas -> {
            this.image.setBounds(
                CANVAS_WIDTH - MARGIN - ICON_WIDTH,
                MARGIN,
                ICON_WIDTH,
                ICON_HEIGHT
            );
            this.image.draw(canvas.graphics());
        });
    }

    @Override
    public void close() {
        this.layer.close();
    }
}
