package net.jr.registry;

import net.jr.Java_reforged;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
        DeferredRegister.create(Registries.SOUND_EVENT, Java_reforged.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOVER = register("ui.hover");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLICK = register("ui.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CHAR = register("ui.char");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_BLOCK = register("ui.block");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_BACK = register("ui.back");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOTBAR_FOCUS = register("ui.hotbar_focus");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_TOAST_NOTIFY = register("ui.toast_new");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMEPLAY_ITEM_POP = register("gameplay.item_pop");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath(Java_reforged.MODID, name)
        ));
    }
}
