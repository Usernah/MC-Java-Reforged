package net.jr.client.runtime.render.pass;

import java.util.ArrayDeque;
import java.util.Deque;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.ui.LocalChatRouting;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.jr.client.runtime.ui.SecondaryWorldLoadingScreen;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.jr.client.runtime.state.HudState;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.hud.PlayerNameHudOverlay;
import net.jr.mixin.runtime.HudSSAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class HudRenderPass {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);

    private HudRenderPass() {
    }

    public static void begin(Hud hud) {
        HudSSAccessor accessor = (HudSSAccessor)hud;
        HudState previousEngineState = new HudState();
        previousEngineState.capture(accessor);
        HudState clientState = LocalClientAcces.render().hud();
        clientState.install(accessor);
        BINDINGS.get().push(new Binding(hud, clientState, previousEngineState));
    }

    public static void end(Hud hud) {
        Deque<Binding> bindings = BINDINGS.get();
        if (bindings.isEmpty() || bindings.peek().hud() != hud) {
            throw new IllegalStateException("Hud state scope is unbalanced");
        }
        Binding binding = bindings.pop();
        HudSSAccessor accessor = (HudSSAccessor)hud;
        binding.clientState().capture(accessor);
        binding.previousEngineState().install(accessor);
        if (bindings.isEmpty()) {
            BINDINGS.remove();
        }
    }

    public static void tick(Gui gui) {
        Hud hud = gui.hud;
        boolean primaryTicked = false;
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = ClientRuntime.INSTANCE.slots().slot(slotId);
            if (!slot.connected() || slot.gameplayState().player() == null || slot.renderState().level() == null) {
                continue;
            }
            if (slotId == 0) {
                primaryTicked = true;
            }
            LocalClientScope.run(slot, client -> {
                begin(hud);
                try {
                    if (client.slotId() == 0) {
                        gui.tick();
                    } else {
                        gui.chatListener().tick();
                        hud.tick(Minecraft.getInstance().isPaused());
                    }
                } finally {
                    end(hud);
                }
            });
        }

        if (!primaryTicked) {
            gui.tick();
        }
    }

    public static void tickChat(ChatComponent chat) {
        LocalChatRouting.tick(chat, Minecraft.getInstance().gui.hud.getGuiTicks());
    }

    public static void extract(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
            if (!LocalScreenManager.slotUiPassCanRender(slot)) {
                continue;
            }
            try (LocalClientScope ignoredClient = LocalClientScope.enter(slot);
                 GuiViewportScope ignoredViewport = GuiViewportScope.enter(graphics)) {
                boolean loadingSecondaryWorld = SecondaryWorldLoadingScreen.shouldPresent(slot);
                boolean hideGameplayHud = LocalClientAcces.screen() instanceof AbstractContainerScreen<?>;
                if (!loadingSecondaryWorld && !hideGameplayHud) {
                    begin(hud);
                    try {
                        hud.extractRenderState(graphics, deltaTracker);
                        ControlHintPipeline.renderHud(graphics);
                        if (SplitScreen.isActive()) {
                            PlayerNameHudOverlay.render(graphics);
                        }
                    } finally {
                        end(hud);
                    }
                }

                LocalScreenManager.extract(minecraft, graphics, deltaTracker.getGameTimeDeltaTicks());
                if (!loadingSecondaryWorld) {
                    hud.extractSavingIndicator(graphics, deltaTracker);
                    hud.extractDebugOverlay(graphics);
                    hud.extractDeferredSubtitles();
                }
            }
        }
    }

    public static Integer guiWidthOrNull() {
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport == null ? null : viewport.guiWidth();
    }

    public static Integer guiHeightOrNull() {
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport == null ? null : viewport.guiHeight();
    }

    private record Binding(Hud hud, HudState clientState, HudState previousEngineState) {
    }
}
