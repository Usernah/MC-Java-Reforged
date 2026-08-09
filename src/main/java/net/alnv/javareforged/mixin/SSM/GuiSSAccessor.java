package net.alnv.javareforged.mixin.SSM;

import javax.annotation.Nullable;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiSSAccessor {
    @Invoker("tick") void splitTest$tickPlayer();
    @Accessor("tickCount") int splitTest$getTickCount();
    @Accessor("tickCount") void splitTest$setTickCount(int value);
    @Accessor("overlayMessageString") @Nullable Component splitTest$getOverlayMessage();
    @Accessor("overlayMessageString") void splitTest$setOverlayMessage(@Nullable Component value);
    @Accessor("overlayMessageTime") int splitTest$getOverlayMessageTime();
    @Accessor("overlayMessageTime") void splitTest$setOverlayMessageTime(int value);
    @Accessor("animateOverlayMessageColor") boolean splitTest$getAnimateOverlayMessageColor();
    @Accessor("animateOverlayMessageColor") void splitTest$setAnimateOverlayMessageColor(boolean value);
    @Accessor("chatDisabledByPlayerShown") boolean splitTest$getChatDisabledByPlayerShown();
    @Accessor("chatDisabledByPlayerShown") void splitTest$setChatDisabledByPlayerShown(boolean value);
    @Accessor("vignetteBrightness") float splitTest$getVignetteBrightness();
    @Accessor("vignetteBrightness") void splitTest$setVignetteBrightness(float value);
    @Accessor("toolHighlightTimer") int splitTest$getToolHighlightTimer();
    @Accessor("toolHighlightTimer") void splitTest$setToolHighlightTimer(int value);
    @Accessor("lastToolHighlight") ItemStack splitTest$getLastToolHighlight();
    @Accessor("lastToolHighlight") void splitTest$setLastToolHighlight(ItemStack value);
    @Accessor("titleTime") int splitTest$getTitleTime();
    @Accessor("titleTime") void splitTest$setTitleTime(int value);
    @Accessor("title") @Nullable Component splitTest$getTitle();
    @Accessor("title") void splitTest$setTitle(@Nullable Component value);
    @Accessor("subtitle") @Nullable Component splitTest$getSubtitle();
    @Accessor("subtitle") void splitTest$setSubtitle(@Nullable Component value);
    @Accessor("titleFadeInTime") int splitTest$getTitleFadeInTime();
    @Accessor("titleFadeInTime") void splitTest$setTitleFadeInTime(int value);
    @Accessor("titleStayTime") int splitTest$getTitleStayTime();
    @Accessor("titleStayTime") void splitTest$setTitleStayTime(int value);
    @Accessor("titleFadeOutTime") int splitTest$getTitleFadeOutTime();
    @Accessor("titleFadeOutTime") void splitTest$setTitleFadeOutTime(int value);
    @Accessor("lastHealth") int splitTest$getLastHealth();
    @Accessor("lastHealth") void splitTest$setLastHealth(int value);
    @Accessor("displayHealth") int splitTest$getDisplayHealth();
    @Accessor("displayHealth") void splitTest$setDisplayHealth(int value);
    @Accessor("lastHealthTime") long splitTest$getLastHealthTime();
    @Accessor("lastHealthTime") void splitTest$setLastHealthTime(long value);
    @Accessor("healthBlinkTime") long splitTest$getHealthBlinkTime();
    @Accessor("healthBlinkTime") void splitTest$setHealthBlinkTime(long value);
    @Accessor("scopeScale") float splitTest$getScopeScale();
    @Accessor("scopeScale") void splitTest$setScopeScale(float value);
}
