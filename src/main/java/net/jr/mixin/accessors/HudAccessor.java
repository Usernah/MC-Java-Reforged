package net.jr.mixin.accessors;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(Hud.class)
public interface HudAccessor {
    @Invoker("extractCameraOverlays")
    void javaReforged$extractCameraEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    @Accessor("tickCount") int javaReforged$getTickCount();
    @Accessor("tickCount") void javaReforged$setTickCount(int value);
    @Accessor("overlayMessageString") @Nullable Component javaReforged$getOverlayMessage();
    @Accessor("overlayMessageString") void javaReforged$setOverlayMessage(@Nullable Component value);
    @Accessor("overlayMessageTime") int javaReforged$getOverlayMessageTime();
    @Accessor("overlayMessageTime") void javaReforged$setOverlayMessageTime(int value);
    @Accessor("animateOverlayMessageColor") boolean javaReforged$getAnimateOverlayMessageColor();
    @Accessor("animateOverlayMessageColor") void javaReforged$setAnimateOverlayMessageColor(boolean value);
    @Accessor("toolHighlightTimer") int javaReforged$getToolHighlightTimer();
    @Accessor("toolHighlightTimer") void javaReforged$setToolHighlightTimer(int value);
    @Accessor("lastToolHighlight") ItemStack javaReforged$getLastToolHighlight();
    @Accessor("lastToolHighlight") void javaReforged$setLastToolHighlight(ItemStack value);
    @Accessor("titleTime") int javaReforged$getTitleTime();
    @Accessor("titleTime") void javaReforged$setTitleTime(int value);
    @Accessor("title") @Nullable Component javaReforged$getTitle();
    @Accessor("title") void javaReforged$setTitle(@Nullable Component value);
    @Accessor("subtitle") @Nullable Component javaReforged$getSubtitle();
    @Accessor("subtitle") void javaReforged$setSubtitle(@Nullable Component value);
    @Accessor("titleFadeInTime") int javaReforged$getTitleFadeInTime();
    @Accessor("titleFadeInTime") void javaReforged$setTitleFadeInTime(int value);
    @Accessor("titleStayTime") int javaReforged$getTitleStayTime();
    @Accessor("titleStayTime") void javaReforged$setTitleStayTime(int value);
    @Accessor("titleFadeOutTime") int javaReforged$getTitleFadeOutTime();
    @Accessor("titleFadeOutTime") void javaReforged$setTitleFadeOutTime(int value);
    @Accessor("lastHealth") int javaReforged$getLastHealth();
    @Accessor("lastHealth") void javaReforged$setLastHealth(int value);
    @Accessor("displayHealth") int javaReforged$getDisplayHealth();
    @Accessor("displayHealth") void javaReforged$setDisplayHealth(int value);
    @Accessor("lastHealthTime") long javaReforged$getLastHealthTime();
    @Accessor("lastHealthTime") void javaReforged$setLastHealthTime(long value);
    @Accessor("healthBlinkTime") long javaReforged$getHealthBlinkTime();
    @Accessor("healthBlinkTime") void javaReforged$setHealthBlinkTime(long value);
    @Accessor("lastBubblePopSoundPlayed") int javaReforged$getLastBubblePopSoundPlayed();
    @Accessor("lastBubblePopSoundPlayed") void javaReforged$setLastBubblePopSoundPlayed(int value);
    @Accessor("scopeScale") float javaReforged$getScopeScale();
    @Accessor("scopeScale") void javaReforged$setScopeScale(float value);
}
