package net.jr.ClientRuntime.state;

import javax.annotation.Nullable;
import net.jr.mixin.SSM.HudSSAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class HudState {
    private int tickCount;
    @Nullable
    private Component overlayMessage;
    private int overlayMessageTime;
    private boolean animateOverlayMessageColor;
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
    private int lastBubblePopSoundPlayed;
    private float scopeScale;

    public void install(HudSSAccessor hud) {
        hud.splitTest$setTickCount(this.tickCount);
        hud.splitTest$setOverlayMessage(this.overlayMessage);
        hud.splitTest$setOverlayMessageTime(this.overlayMessageTime);
        hud.splitTest$setAnimateOverlayMessageColor(this.animateOverlayMessageColor);
        hud.splitTest$setVignetteBrightness(this.vignetteBrightness);
        hud.splitTest$setToolHighlightTimer(this.toolHighlightTimer);
        hud.splitTest$setLastToolHighlight(this.lastToolHighlight);
        hud.splitTest$setTitleTime(this.titleTime);
        hud.splitTest$setTitle(this.title);
        hud.splitTest$setSubtitle(this.subtitle);
        hud.splitTest$setTitleFadeInTime(this.titleFadeInTime);
        hud.splitTest$setTitleStayTime(this.titleStayTime);
        hud.splitTest$setTitleFadeOutTime(this.titleFadeOutTime);
        hud.splitTest$setLastHealth(this.lastHealth);
        hud.splitTest$setDisplayHealth(this.displayHealth);
        hud.splitTest$setLastHealthTime(this.lastHealthTime);
        hud.splitTest$setHealthBlinkTime(this.healthBlinkTime);
        hud.splitTest$setLastBubblePopSoundPlayed(this.lastBubblePopSoundPlayed);
        hud.splitTest$setScopeScale(this.scopeScale);
    }

    public void capture(HudSSAccessor hud) {
        this.tickCount = hud.splitTest$getTickCount();
        this.overlayMessage = hud.splitTest$getOverlayMessage();
        this.overlayMessageTime = hud.splitTest$getOverlayMessageTime();
        this.animateOverlayMessageColor = hud.splitTest$getAnimateOverlayMessageColor();
        this.vignetteBrightness = hud.splitTest$getVignetteBrightness();
        this.toolHighlightTimer = hud.splitTest$getToolHighlightTimer();
        this.lastToolHighlight = hud.splitTest$getLastToolHighlight();
        this.titleTime = hud.splitTest$getTitleTime();
        this.title = hud.splitTest$getTitle();
        this.subtitle = hud.splitTest$getSubtitle();
        this.titleFadeInTime = hud.splitTest$getTitleFadeInTime();
        this.titleStayTime = hud.splitTest$getTitleStayTime();
        this.titleFadeOutTime = hud.splitTest$getTitleFadeOutTime();
        this.lastHealth = hud.splitTest$getLastHealth();
        this.displayHealth = hud.splitTest$getDisplayHealth();
        this.lastHealthTime = hud.splitTest$getLastHealthTime();
        this.healthBlinkTime = hud.splitTest$getHealthBlinkTime();
        this.lastBubblePopSoundPlayed = hud.splitTest$getLastBubblePopSoundPlayed();
        this.scopeScale = hud.splitTest$getScopeScale();
    }

    public void clear() {
        this.tickCount = 0;
        this.overlayMessage = null;
        this.overlayMessageTime = 0;
        this.animateOverlayMessageColor = false;
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
        this.lastBubblePopSoundPlayed = 0;
        this.scopeScale = 0.0F;
    }
}
