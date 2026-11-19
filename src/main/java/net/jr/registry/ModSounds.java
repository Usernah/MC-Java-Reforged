package net.jr.registry;

import net.jr.Java_reforged;
import net.jr.api.client.render.font.FontRegister;
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
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CRAFT = register("ui.craft");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_BACK = register("ui.back");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOTBAR_FOCUS = register("ui.hotbar_focus");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_TOAST_NOTIFY = register("ui.toast_new");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_UI_OVERLAY_OPEN = register("system_ui.overlay_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_UI_OVERLAY_CLOSE = register("system_ui.overlay_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_UI_FOCUS = register("system_ui.focus");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_UI_SELECT_BUTTON = register("system_ui.select_btn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_UI_SELECT_CHARACTER = register("system_ui.select_char");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAMEPLAY_ITEM_POP = register("gameplay.item_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_CELEBRATION = register("minigames.battle.celebration");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_CELEBRATION2 = register("minigames.battle.celebration2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_CHEST = register("minigames.battle.chest");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_CHEST2 = register("minigames.battle.chest2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_DEATH = register("minigames.battle.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_DEATH2 = register("minigames.battle.death2");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_GAMESTART = register("minigames.battle.gamestart");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_GRACE = register("minigames.battle.grace");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_GRACEZERO = register("minigames.battle.gracezero");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_BATTLE_SHOWDOWN = register("minigames.battle.showdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_LOBBY_LOBBY = register("minigames.lobby.lobby");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_LOBBY_LOBBYACCENT = register("minigames.lobby.lobbyaccent");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_LOBBY_LOBBYZERO = register("minigames.lobby.lobbyzero");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_LOBBY_STARTCD = register("minigames.lobby.startcd");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGAMES_MUSIC_BATTLE_VANILLA = register("minigames.music.battle.vanilla");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath(Java_reforged.MODID, name)
        ));
    }
}
