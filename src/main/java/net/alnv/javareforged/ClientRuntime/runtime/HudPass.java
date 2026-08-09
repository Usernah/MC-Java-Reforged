package net.alnv.javareforged.ClientRuntime.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import net.alnv.javareforged.mixin.SSM.GuiSSAccessor;
import net.alnv.javareforged.client.ui.hint.ControlHintPipeline;
import net.alnv.javareforged.client.ui.hud.HudCompositeTarget;
import net.alnv.javareforged.client.ui.hud.HudTransparency;
import net.alnv.javareforged.minigames.battle.client.BattleHudRenderer;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlots;
import net.alnv.javareforged.ClientRuntime.state.HudState;
import net.alnv.javareforged.ClientRuntime.test.InputSlotProbe;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class HudPass {
    private static final ThreadLocal<Deque<Binding>> BINDINGS = ThreadLocal.withInitial(ArrayDeque::new);
    private HudPass() {
    }

    public static void begin(Gui gui) {
        GuiSSAccessor accessor = (GuiSSAccessor)gui;
        HudState previousEngineState = new HudState();
        previousEngineState.capture(accessor);
        HudState clientState = Client.render().hud();
        clientState.install(accessor);
        BINDINGS.get().push(new Binding(gui, clientState, previousEngineState));
    }

    public static void end(Gui gui) {
        Deque<Binding> bindings = BINDINGS.get();
        if (bindings.isEmpty() || bindings.peek().gui() != gui) {
            throw new IllegalStateException("Gui state scope is unbalanced");
        }
        Binding binding = bindings.pop();
        GuiSSAccessor accessor = (GuiSSAccessor)gui;
        binding.clientState().capture(accessor);
        binding.previousEngineState().install(accessor);
        if (bindings.isEmpty()) {
            BINDINGS.remove();
        }
    }

    public static void tick(Gui gui, boolean pause) {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (!slot.connected() || slot.gameplayState().player() == null || slot.renderState().level() == null) {
                continue;
            }
            final int currentSlotId = slotId;
            LocalClientScope.run(slot, slotContext -> {
                begin(gui);
                try {
                    if (slotContext.id() == 0) {
                        gui.tick(pause);
                    } else if (!pause) {
                        ((GuiSSAccessor)gui).splitTest$tickPlayer();
                    }
                } finally {
                    end(gui);
                }
            });
        }
    }

    public static void tickChat(ChatComponent chat) {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId == null || slotId == 0) {
            chat.tick();
        }
    }

    public static void render(Gui gui, GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
                if (!canRender(slot)) {
                    continue;
                }
                try (LocalClientScope ignored = LocalClientScope.enter(slot)) {
                    renderSlot(minecraft, gui, graphics, deltaTracker);
                }
            }
        } finally {
            restoreFullGui(minecraft);
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

    private static void renderSlot(
        Minecraft minecraft,
        Gui gui,
        GuiGraphics graphics,
        DeltaTracker deltaTracker
    ) {
        LocalClient client = Client.current();
        PlayerSlot slot = client.rawSlot();
        try (GuiViewportScope ignored = GuiViewportScope.enter()) {
            HudCompositeTarget.discardActiveCapture();
            boolean guiScopeOpen = false;
            try {
                boolean hideGameplayHud = shouldHideGameplayHudForCurrentClient();
                if (!hideGameplayHud) {
                    begin(gui);
                    guiScopeOpen = true;
                    gui.render(graphics, deltaTracker);
                    if (HudCompositeTarget.isCapturing()) {
                        ControlHintPipeline.renderHud(graphics);
                        HudCompositeTarget.endAndDraw(graphics, HudTransparency.hudAlpha());
                    }
                }
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

                if (!hideGameplayHud) {
                    BattleHudRenderer.render(graphics);
                }

                Screens.render(
                        minecraft,
                        graphics,
                        deltaTracker.getGameTimeDeltaTicks()
                );
                TerrainDebug.renderOverlay(graphics, slot);
                InputSlotProbe.renderOverlay(graphics, slot);

                graphics.flush();
            } finally {
                HudCompositeTarget.discardActiveCapture();
                if (guiScopeOpen) {
                    end(gui);
                }
            }
        }
    }

    private static void restoreFullGui(Minecraft minecraft) {
        minecraft.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(0, 0, minecraft.getMainRenderTarget().viewWidth, minecraft.getMainRenderTarget().viewHeight);
        RenderSystem.disableScissor();
        setGuiProjection(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    private static void setGuiProjection(int width, int height) {
        float farPlane = net.neoforged.neoforge.client.ClientHooks.getGuiFarPlane();
        Matrix4f projection = new Matrix4f().setOrtho(0.0F, (float)width, (float)height, 0.0F, 1000.0F, farPlane);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
    }

    private static boolean canRender(PlayerSlot slot) {
        return Screens.slotUiPassCanRender(slot);
    }

    private static boolean shouldHideGameplayHudForCurrentClient() {
        return Client.screen() instanceof AbstractContainerScreen<?>;
    }

    private record Binding(Gui gui, HudState clientState, HudState previousEngineState) {
    }
}
