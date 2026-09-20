package net.jr.client.runtime.state;

import com.mojang.datafixers.util.Pair;
import javax.annotation.Nullable;
import net.jr.mixin.accessors.HudAccessor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
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
    private Pair<?, ContextualBar> contextualInfoBar = emptyContextualInfoBar();

    public void install(HudAccessor hud) {
        hud.javaReforged$setTickCount(this.tickCount);
        hud.javaReforged$setOverlayMessage(this.overlayMessage);
        hud.javaReforged$setOverlayMessageTime(this.overlayMessageTime);
        hud.javaReforged$setAnimateOverlayMessageColor(this.animateOverlayMessageColor);
        hud.javaReforged$setToolHighlightTimer(this.toolHighlightTimer);
        hud.javaReforged$setLastToolHighlight(this.lastToolHighlight);
        hud.javaReforged$setTitleTime(this.titleTime);
        hud.javaReforged$setTitle(this.title);
        hud.javaReforged$setSubtitle(this.subtitle);
        hud.javaReforged$setTitleFadeInTime(this.titleFadeInTime);
        hud.javaReforged$setTitleStayTime(this.titleStayTime);
        hud.javaReforged$setTitleFadeOutTime(this.titleFadeOutTime);
        hud.javaReforged$setLastHealth(this.lastHealth);
        hud.javaReforged$setDisplayHealth(this.displayHealth);
        hud.javaReforged$setLastHealthTime(this.lastHealthTime);
        hud.javaReforged$setHealthBlinkTime(this.healthBlinkTime);
        hud.javaReforged$setLastBubblePopSoundPlayed(this.lastBubblePopSoundPlayed);
        hud.javaReforged$setScopeScale(this.scopeScale);
        hud.javaReforged$setContextualInfoBar(this.contextualInfoBar);
    }

    public void capture(HudAccessor hud) {
        this.tickCount = hud.javaReforged$getTickCount();
        this.overlayMessage = hud.javaReforged$getOverlayMessage();
        this.overlayMessageTime = hud.javaReforged$getOverlayMessageTime();
        this.animateOverlayMessageColor = hud.javaReforged$getAnimateOverlayMessageColor();
        this.toolHighlightTimer = hud.javaReforged$getToolHighlightTimer();
        this.lastToolHighlight = hud.javaReforged$getLastToolHighlight();
        this.titleTime = hud.javaReforged$getTitleTime();
        this.title = hud.javaReforged$getTitle();
        this.subtitle = hud.javaReforged$getSubtitle();
        this.titleFadeInTime = hud.javaReforged$getTitleFadeInTime();
        this.titleStayTime = hud.javaReforged$getTitleStayTime();
        this.titleFadeOutTime = hud.javaReforged$getTitleFadeOutTime();
        this.lastHealth = hud.javaReforged$getLastHealth();
        this.displayHealth = hud.javaReforged$getDisplayHealth();
        this.lastHealthTime = hud.javaReforged$getLastHealthTime();
        this.healthBlinkTime = hud.javaReforged$getHealthBlinkTime();
        this.lastBubblePopSoundPlayed = hud.javaReforged$getLastBubblePopSoundPlayed();
        this.scopeScale = hud.javaReforged$getScopeScale();
        this.contextualInfoBar = hud.javaReforged$getContextualInfoBar();
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
        this.contextualInfoBar = emptyContextualInfoBar();
    }

    public float vignetteBrightness() {
        return this.vignetteBrightness;
    }

    public void setVignetteBrightness(float vignetteBrightness) {
        this.vignetteBrightness = vignetteBrightness;
    }

    private static Pair<?, ContextualBar> emptyContextualInfoBar() {
        // A null discriminator forces vanilla to resolve and construct the
        // correct contextual bar under the currently bound local client.
        return Pair.of(null, ContextualBar.EMPTY);
    }
}
