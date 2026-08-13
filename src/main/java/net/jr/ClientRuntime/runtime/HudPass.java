package net.jr.ClientRuntime.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.jr.ClientRuntime.state.HudState;
import net.jr.ClientRuntime.test.InputSlotProbe;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.mixin.SSM.HudSSAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class HudPass {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);

    private HudPass() {
    }

    public static void begin(Hud hud) {
        HudSSAccessor accessor = (HudSSAccessor)hud;
        HudState previousEngineState = new HudState();
        previousEngineState.capture(accessor);
        HudState clientState = Client.render().hud();
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
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (!slot.connected() || slot.gameplayState().player() == null || slot.renderState().level() == null) {
                continue;
            }
            if (slotId == 0) {
                primaryTicked = true;
            }
            LocalClientScope.run(slot, slotContext -> {
                begin(hud);
                try {
                    if (slotContext.id() == 0) {
                        gui.tick();
                    } else {
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
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId == null || slotId == 0) {
            chat.tick();
        }
    }

    public static void extract(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            if (!Screens.slotUiPassCanRender(slot)) {
                continue;
            }
            try (LocalClientScope ignoredClient = LocalClientScope.enter(slot);
                 GuiViewportScope ignoredViewport = GuiViewportScope.enter(graphics)) {
                boolean loadingSecondaryWorld = SecondaryWorldLoadingScreen.shouldPresent(slot);
                boolean hideGameplayHud = Client.screen() instanceof AbstractContainerScreen<?>;
                if (!loadingSecondaryWorld && !hideGameplayHud) {
                    begin(hud);
                    try {
                        hud.extractRenderState(graphics, deltaTracker);
                        ControlHintPipeline.renderHud(graphics);
                    } finally {
                        end(hud);
                    }
                }

                Screens.extract(minecraft, graphics, deltaTracker.getGameTimeDeltaTicks());
                if (!loadingSecondaryWorld) {
                    hud.extractSavingIndicator(graphics, deltaTracker);
                    hud.extractDebugOverlay(graphics);
                    InputSlotProbe.renderOverlay(graphics, slot);
                    hud.extractDeferredSubtitles();
                }
            }
        }
    }

    public static Integer guiWidthOrNull() {
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        return viewport == null ? null : viewport.guiWidth();
    }

    public static Integer guiHeightOrNull() {
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        return viewport == null ? null : viewport.guiHeight();
    }

    private record Binding(Hud hud, HudState clientState, HudState previousEngineState) {
    }
}
