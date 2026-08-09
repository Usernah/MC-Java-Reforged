package net.alnv.javareforged.ClientRuntime.state;

import javax.annotation.Nullable;
import net.alnv.javareforged.mixin.SSM.GuiSSAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class HudState {
    private int tickCount;
    @Nullable
    private Component overlayMessage;
    private int overlayMessageTime;
    private boolean animateOverlayMessageColor;
    private boolean chatDisabledByPlayerShown;
    private float vignetteBrightness = 1.0F;
    private int toolHighlightTimer;
    private ItemStack lastToolHighlight = ItemStack.EMPTY;
    private int titleTime;
    @Nullable
    private Component title;
    @Nullable
    private Component subtitle;
    private int titleFadeInTime = 10;
    private int titleStayTime = 70;
    private int titleFadeOutTime = 20;
    private int lastHealth;
    private int displayHealth;
    private long lastHealthTime;
    private long healthBlinkTime;
    private float scopeScale;

    public void install(GuiSSAccessor gui) {
        gui.splitTest$setTickCount(this.tickCount);
        gui.splitTest$setOverlayMessage(this.overlayMessage);
        gui.splitTest$setOverlayMessageTime(this.overlayMessageTime);
        gui.splitTest$setAnimateOverlayMessageColor(this.animateOverlayMessageColor);
        gui.splitTest$setChatDisabledByPlayerShown(this.chatDisabledByPlayerShown);
        gui.splitTest$setVignetteBrightness(this.vignetteBrightness);
        gui.splitTest$setToolHighlightTimer(this.toolHighlightTimer);
        gui.splitTest$setLastToolHighlight(this.lastToolHighlight);
        gui.splitTest$setTitleTime(this.titleTime);
        gui.splitTest$setTitle(this.title);
        gui.splitTest$setSubtitle(this.subtitle);
        gui.splitTest$setTitleFadeInTime(this.titleFadeInTime);
        gui.splitTest$setTitleStayTime(this.titleStayTime);
        gui.splitTest$setTitleFadeOutTime(this.titleFadeOutTime);
        gui.splitTest$setLastHealth(this.lastHealth);
        gui.splitTest$setDisplayHealth(this.displayHealth);
        gui.splitTest$setLastHealthTime(this.lastHealthTime);
        gui.splitTest$setHealthBlinkTime(this.healthBlinkTime);
        gui.splitTest$setScopeScale(this.scopeScale);
    }

    public void capture(GuiSSAccessor gui) {
        this.tickCount = gui.splitTest$getTickCount();
        this.overlayMessage = gui.splitTest$getOverlayMessage();
        this.overlayMessageTime = gui.splitTest$getOverlayMessageTime();
        this.animateOverlayMessageColor = gui.splitTest$getAnimateOverlayMessageColor();
        this.chatDisabledByPlayerShown = gui.splitTest$getChatDisabledByPlayerShown();
        this.vignetteBrightness = gui.splitTest$getVignetteBrightness();
        this.toolHighlightTimer = gui.splitTest$getToolHighlightTimer();
        this.lastToolHighlight = gui.splitTest$getLastToolHighlight();
        this.titleTime = gui.splitTest$getTitleTime();
        this.title = gui.splitTest$getTitle();
        this.subtitle = gui.splitTest$getSubtitle();
        this.titleFadeInTime = gui.splitTest$getTitleFadeInTime();
        this.titleStayTime = gui.splitTest$getTitleStayTime();
        this.titleFadeOutTime = gui.splitTest$getTitleFadeOutTime();
        this.lastHealth = gui.splitTest$getLastHealth();
        this.displayHealth = gui.splitTest$getDisplayHealth();
        this.lastHealthTime = gui.splitTest$getLastHealthTime();
        this.healthBlinkTime = gui.splitTest$getHealthBlinkTime();
        this.scopeScale = gui.splitTest$getScopeScale();
    }

    public void clear() {
        this.tickCount = 0;
        this.overlayMessage = null;
        this.overlayMessageTime = 0;
        this.animateOverlayMessageColor = false;
        this.chatDisabledByPlayerShown = false;
        this.vignetteBrightness = 1.0F;
        this.toolHighlightTimer = 0;
        this.lastToolHighlight = ItemStack.EMPTY;
        this.titleTime = 0;
        this.title = null;
        this.subtitle = null;
        this.titleFadeInTime = 10;
        this.titleStayTime = 70;
        this.titleFadeOutTime = 20;
        this.lastHealth = 0;
        this.displayHealth = 0;
        this.lastHealthTime = 0L;
        this.healthBlinkTime = 0L;
        this.scopeScale = 0.0F;
    }
}
