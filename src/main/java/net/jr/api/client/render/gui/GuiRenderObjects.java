package net.jr.api.client.render.gui;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


public final class GuiRenderObjects {
    private static final List<HudRenderObject> HUD = new CopyOnWriteArrayList<>();
    private static final List<HudRenderObject> HUD_FRAMEBUFFER = new CopyOnWriteArrayList<>();
    private static final List<HudRenderObject> HUD_FULL_HD_FRAMEBUFFER = new CopyOnWriteArrayList<>();
    private static final List<BottomHudRenderObject> BOTTOM_HUD = new CopyOnWriteArrayList<>();
    private static final List<HudRenderObject> HUD_TAIL = new CopyOnWriteArrayList<>();
    private static final EnumMap<GuiRenderPhase, List<IGuiRenderObject<GuiRenderContext>>> LOCAL =
        new EnumMap<>(GuiRenderPhase.class);
    private static final EnumMap<GuiRenderPhase, List<IGlobalRenderObject>> GLOBAL =
        new EnumMap<>(GuiRenderPhase.class);

    static {
        for (GuiRenderPhase phase : GuiRenderPhase.values()) {
            LOCAL.put(phase, new CopyOnWriteArrayList<>());
            GLOBAL.put(phase, new CopyOnWriteArrayList<>());
        }
    }

    private GuiRenderObjects() {
    }

    public static void registerHud(HudRenderObject object) {
        HUD.add(Objects.requireNonNull(object, "object"));
    }


    public static void registerHudFramebuffer(HudRenderObject object) {
        HUD_FRAMEBUFFER.add(Objects.requireNonNull(object, "object"));
    }

    public static void registerHudFullHdFramebuffer(HudRenderObject object) {
        HUD_FULL_HD_FRAMEBUFFER.add(Objects.requireNonNull(object, "object"));
    }


    public static void registerBottomHud(BottomHudRenderObject object) {
        BOTTOM_HUD.add(Objects.requireNonNull(object, "object"));
    }


    public static void registerHudTail(HudRenderObject object) {
        HUD_TAIL.add(Objects.requireNonNull(object, "object"));
    }

    public static void register(GuiRenderPhase phase, IGuiRenderObject<GuiRenderContext> object) {
        Objects.requireNonNull(phase, "phase");
        if (phase != GuiRenderPhase.OVERLAY && phase != GuiRenderPhase.SCREEN
            && phase != GuiRenderPhase.SCREEN_OVERLAY && phase != GuiRenderPhase.TOAST) {
            throw new IllegalArgumentException("Phase does not accept a local GUI object: " + phase);
        }
        LOCAL.get(phase).add(Objects.requireNonNull(object, "object"));
    }

    public static void registerGlobal(GuiRenderPhase phase, IGlobalRenderObject object) {
        Objects.requireNonNull(phase, "phase");
        if (phase != GuiRenderPhase.VANILLA_OVERLAY && phase != GuiRenderPhase.TOAST
            && phase != GuiRenderPhase.GLOBAL) {
            throw new IllegalArgumentException("Phase does not accept a global GUI object: " + phase);
        }
        GLOBAL.get(phase).add(Objects.requireNonNull(object, "object"));
    }

    public static void renderHud(HudRenderContext context) {
        for (HudRenderObject object : HUD) {
            object.render(context);
        }
    }

    public static void renderHudFramebuffer(HudRenderContext context) {
        for (HudRenderObject object : HUD_FRAMEBUFFER) {
            object.render(context);
        }
    }

    public static void renderHudFullHdFramebuffer(HudRenderContext context) {
        for (HudRenderObject object : HUD_FULL_HD_FRAMEBUFFER) {
            object.render(context);
        }
    }

    public static void renderBottomHud(BottomHudRenderContext context) {
        for (BottomHudRenderObject object : BOTTOM_HUD) {
            object.render(context);
        }
    }

    public static void renderHudTail(HudRenderContext context) {
        for (HudRenderObject object : HUD_TAIL) {
            object.render(context);
        }
    }

    public static void render(GuiRenderPhase phase, GuiRenderContext context) {
        for (IGuiRenderObject<GuiRenderContext> object : LOCAL.get(phase)) {
            object.render(context);
        }
    }

    public static void renderGlobal(GuiRenderPhase phase, GlobalRenderContext context) {
        for (IGlobalRenderObject object : GLOBAL.get(phase)) {
            object.render(context);
        }
    }
}
