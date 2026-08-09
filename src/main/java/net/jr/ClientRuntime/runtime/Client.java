package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.state.GameplayState;
import net.jr.ClientRuntime.state.InputState;
import net.jr.ClientRuntime.state.RenderState;
import net.jr.ClientRuntime.state.ScreenState;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Gui;
import net.jr.mixin.SSM.GuiRawScreenAccessor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.HitResult;

/**
 * Unified facade for the current local client and explicit local-client state access.
 *
 * <p>Methods without an id use the client already selected by the high-level
 * execution boundary. Id overloads address one client directly without changing
 * the current client. Full vanilla execution still belongs to ClientBoundary.</p>
 */
public final class Client {
    public static final int MAX_CLIENTS = PlayerSlots.MAX_SLOTS;

    private Client() {
    }

    public static LocalClient current() {
        return LocalClientScope.currentClient();
    }

    @Nullable
    public static LocalClient currentOrNull() {
        return LocalClientScope.currentClientOrNull();
    }

    public static int slotId() {
        return current().slotId();
    }

    public static int connectedCount() {
        int count = 0;
        for (int clientId = 0; clientId < MAX_CLIENTS; clientId++) {
            if (connected(clientId)) {
                count++;
            }
        }
        return count;
    }

    /** Creates the next local Client and returns its explicit id. */
    public static int joinNext(Minecraft minecraft) {
        return LocalPlayers.INSTANCE.joinNextClient(minecraft);
    }

    public static boolean connected() {
        return current().connected();
    }

    public static boolean connected(int clientId) {
        return client(clientId).connected();
    }

    public static boolean visible() {
        return current().visible();
    }

    public static boolean visible(int clientId) {
        return client(clientId).visible();
    }

    public static boolean drawable() {
        return current().drawable();
    }

    public static boolean drawable(int clientId) {
        return client(clientId).drawable();
    }

    public static boolean worldReady() {
        return current().worldReady();
    }

    public static boolean worldReady(int clientId) {
        return client(clientId).worldReady();
    }

    public static boolean gameplayReady() {
        return current().gameplayReady();
    }

    public static boolean gameplayReady(int clientId) {
        return client(clientId).gameplayReady();
    }

    public static RenderState render() {
        return current().render();
    }

    public static RenderState render(int clientId) {
        return client(clientId).render();
    }

    public static GameplayState gameplay() {
        return current().gameplay();
    }

    public static GameplayState gameplay(int clientId) {
        return client(clientId).gameplay();
    }

    public static ScreenState ui() {
        return current().ui();
    }

    public static ScreenState ui(int clientId) {
        return client(clientId).ui();
    }

    public static InputState input() {
        return current().input();
    }

    public static InputState input(int clientId) {
        return client(clientId).input();
    }

    public static LocalClientCursor cursor() {
        return current().cursor();
    }

    public static LocalClientCursor cursor(int clientId) {
        return client(clientId).cursor();
    }

    public static boolean hasViewport() {
        return current().hasViewport();
    }

    public static boolean hasViewport(int clientId) {
        return client(clientId).hasViewport();
    }

    @Nullable
    public static ViewportArea viewportOrNull() {
        return current().viewportOrNull();
    }

    @Nullable
    public static ViewportArea viewportOrNull(int clientId) {
        return client(clientId).viewportOrNull();
    }

    public static ViewportArea viewport() {
        return current().viewport();
    }

    public static ViewportArea viewport(int clientId) {
        return client(clientId).viewport();
    }

    @Nullable
    public static ClientLevel level() {
        return current().level();
    }

    @Nullable
    public static ClientLevel level(int clientId) {
        return client(clientId).level();
    }

    public static Camera camera() {
        return current().camera();
    }

    public static Camera camera(int clientId) {
        return client(clientId).camera();
    }

    /** Bridge used by the runtime transformer for GameRenderer.mainCamera. */
    public static Camera camera(GameRenderer gameRenderer) {
        LocalClient client = currentOrNull();
        return client != null ? client.camera() : gameRenderer.mainCamera();
    }

    @Nullable
    public static Frustum frustum() {
        return current().frustum();
    }

    @Nullable
    public static Frustum frustum(int clientId) {
        return client(clientId).frustum();
    }

    @Nullable
    public static Entity cameraEntity() {
        return current().cameraEntity();
    }

    @Nullable
    public static Entity cameraEntity(int clientId) {
        return client(clientId).cameraEntity();
    }

    @Nullable
    public static Entity crosshairPickEntity() {
        return current().crosshairPickEntity();
    }

    @Nullable
    public static Entity crosshairPickEntity(int clientId) {
        return client(clientId).crosshairPickEntity();
    }

    @Nullable
    public static HitResult hitResult() {
        return current().hitResult();
    }

    @Nullable
    public static HitResult hitResult(int clientId) {
        return client(clientId).hitResult();
    }

    @Nullable
    public static LocalPlayer player() {
        return current().player();
    }

    @Nullable
    public static LocalPlayer player(int clientId) {
        return client(clientId).player();
    }

    @Nullable
    public static LocalPlayer player(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null ? client.player() : minecraft.player;
    }

    @Nullable
    public static MultiPlayerGameMode gameMode() {
        return current().gameMode();
    }

    @Nullable
    public static MultiPlayerGameMode gameMode(int clientId) {
        return client(clientId).gameMode();
    }

    @Nullable
    public static MultiPlayerGameMode gameMode(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null ? client.gameMode() : minecraft.gameMode;
    }

    @Nullable
    public static Screen screen() {
        return current().screen();
    }

    @Nullable
    public static Screen screen(int clientId) {
        return client(clientId).screen();
    }

    @Nullable
    public static Screen screen(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null ? client.screen() : minecraft.gui.screen();
    }

    @Nullable
    public static Screen screen(Gui gui) {
        LocalClient client = currentOrNull();
        return client != null ? client.screen() : ((GuiRawScreenAccessor)gui).splitTest$getRawScreen();
    }

    @Nullable
    public static AbstractContainerMenu menu() {
        return current().menu();
    }

    @Nullable
    public static AbstractContainerMenu menu(int clientId) {
        return client(clientId).menu();
    }

    public static boolean noRender() {
        return current().render().noRender();
    }

    public static boolean noRender(int clientId) {
        return client(clientId).render().noRender();
    }

    public static float fov() {
        return current().render().fov();
    }

    public static float fov(int clientId) {
        return client(clientId).render().fov();
    }

    public static float aspectRatio() {
        return current().render().aspectRatio();
    }

    public static float aspectRatio(int clientId) {
        return client(clientId).render().aspectRatio();
    }

    public static int rainSoundTime() {
        return current().render().rainSoundTime();
    }

    public static int rainSoundTime(int clientId) {
        return client(clientId).render().rainSoundTime();
    }

    public static int rightClickDelay() {
        return current().gameplay().rightClickDelay();
    }

    public static int rightClickDelay(int clientId) {
        return client(clientId).gameplay().rightClickDelay();
    }

    public static int missTime() {
        return current().gameplay().missTime();
    }

    public static int missTime(int clientId) {
        return client(clientId).gameplay().missTime();
    }

    @Nullable
    public static ClientLevel level(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null ? client.level() : minecraft.level;
    }

    @Nullable
    public static Entity cameraEntity(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        if (client == null) {
            return liveEntityOrNull(minecraft.getCameraEntity());
        }
        Entity cameraEntity = liveEntityOrNull(client.cameraEntity());
        return cameraEntity != null ? cameraEntity : liveEntityOrNull(client.player());
    }

    @Nullable
    public static Entity crosshairPickEntity(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null ? client.crosshairPickEntity() : minecraft.crosshairPickEntity;
    }

    @Nullable
    public static HitResult hitResult(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null ? client.hitResult() : minecraft.hitResult;
    }

    public static boolean noRender(Minecraft minecraft) {
        LocalClient client = currentOrNull();
        return client != null && client.render().noRender();
    }

    public static void setLevel(@Nullable ClientLevel level) {
        current().render().setLevel(level);
    }

    public static void setLevel(int clientId, @Nullable ClientLevel level) {
        client(clientId).render().setLevel(level);
    }

    /** Bridge used by the runtime transformer for Minecraft.level writes. */
    public static void setLevel(Minecraft minecraft, @Nullable ClientLevel level) {
        LocalClient client = currentOrNull();
        if (client == null) {
            minecraft.level = level;
            return;
        }
        if (level == null) {
            client.rawSlot().clearWorldBinding();
        } else {
            client.render().setLevel(level);
        }
    }

    public static void setCamera(Camera camera) {
        current().render().setCamera(camera);
    }

    public static void setCamera(int clientId, Camera camera) {
        client(clientId).render().setCamera(camera);
    }

    public static void setFrustum(@Nullable Frustum frustum) {
        current().render().setFrustum(frustum);
    }

    public static void setFrustum(int clientId, @Nullable Frustum frustum) {
        client(clientId).render().setFrustum(frustum);
    }

    public static void setCameraEntity(@Nullable Entity entity) {
        current().render().setCameraEntity(entity);
    }

    public static void setCameraEntity(int clientId, @Nullable Entity entity) {
        client(clientId).render().setCameraEntity(entity);
    }

    /** Bridge used by the runtime transformer for Minecraft.setCameraEntity calls. */
    public static void setCameraEntity(Minecraft minecraft, @Nullable Entity entity) {
        LocalClient client = currentOrNull();
        if (client != null) {
            client.render().setCameraEntity(entity);
        }
        minecraft.setCameraEntity(entity);
    }

    public static void setCrosshairPickEntity(@Nullable Entity entity) {
        current().render().setCrosshairPickEntity(entity);
    }

    public static void setCrosshairPickEntity(int clientId, @Nullable Entity entity) {
        client(clientId).render().setCrosshairPickEntity(entity);
    }

    public static void setCrosshairPickEntity(Minecraft minecraft, @Nullable Entity entity) {
        LocalClient client = currentOrNull();
        if (client == null) {
            minecraft.crosshairPickEntity = entity;
            return;
        }
        client.render().setCrosshairPickEntity(entity);
    }

    public static void setHitResult(@Nullable HitResult hitResult) {
        current().render().setHitResult(hitResult);
    }

    public static void setHitResult(int clientId, @Nullable HitResult hitResult) {
        client(clientId).render().setHitResult(hitResult);
    }

    public static void setHitResult(Minecraft minecraft, @Nullable HitResult hitResult) {
        LocalClient client = currentOrNull();
        if (client == null) {
            minecraft.hitResult = hitResult;
            return;
        }
        client.render().setHitResult(hitResult);
    }

    public static void setNoRender(boolean noRender) {
        current().render().setNoRender(noRender);
    }

    public static void setNoRender(int clientId, boolean noRender) {
        client(clientId).render().setNoRender(noRender);
    }

    public static void setFov(float fov) {
        current().render().setFov(fov);
    }

    public static void setFov(int clientId, float fov) {
        client(clientId).render().setFov(fov);
    }

    public static void setAspectRatio(float aspectRatio) {
        current().render().setAspectRatio(aspectRatio);
    }

    public static void setAspectRatio(int clientId, float aspectRatio) {
        client(clientId).render().setAspectRatio(aspectRatio);
    }

    public static void setRainSoundTime(int rainSoundTime) {
        current().render().setRainSoundTime(rainSoundTime);
    }

    public static void setRainSoundTime(int clientId, int rainSoundTime) {
        client(clientId).render().setRainSoundTime(rainSoundTime);
    }

    public static void setPlayer(@Nullable LocalPlayer player) {
        current().gameplay().setPlayer(player);
    }

    public static void setPlayer(int clientId, @Nullable LocalPlayer player) {
        client(clientId).gameplay().setPlayer(player);
    }

    /** Bridge used by the runtime transformer for Minecraft.player writes. */
    public static void setPlayer(Minecraft minecraft, @Nullable LocalPlayer player) {
        LocalClient client = currentOrNull();
        if (client == null) {
            minecraft.player = player;
        } else {
            client.gameplay().setPlayer(player);
        }
    }

    public static void setGameMode(@Nullable MultiPlayerGameMode gameMode) {
        current().gameplay().setGameMode(gameMode);
    }

    public static void setGameMode(int clientId, @Nullable MultiPlayerGameMode gameMode) {
        client(clientId).gameplay().setGameMode(gameMode);
    }

    /** Bridge used by the runtime transformer for Minecraft.gameMode writes. */
    public static void setGameMode(Minecraft minecraft, @Nullable MultiPlayerGameMode gameMode) {
        LocalClient client = currentOrNull();
        if (client == null) {
            minecraft.gameMode = gameMode;
        } else {
            client.gameplay().setGameMode(gameMode);
        }
    }

    public static void setRightClickDelay(int rightClickDelay) {
        current().gameplay().setRightClickDelay(rightClickDelay);
    }

    public static void setRightClickDelay(int clientId, int rightClickDelay) {
        client(clientId).gameplay().setRightClickDelay(rightClickDelay);
    }

    public static void setMissTime(int missTime) {
        current().gameplay().setMissTime(missTime);
    }

    public static void setMissTime(int clientId, int missTime) {
        client(clientId).gameplay().setMissTime(missTime);
    }

    public static void setScreen(@Nullable Screen screen) {
        current();
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static void setScreen(int clientId, @Nullable Screen screen) {
        LocalClient target = client(clientId);
        LocalClient active = currentOrNull();
        if (active != null && active.slotId() == clientId) {
            setScreen(screen);
            return;
        }
        ClientBoundary.runForSlot(target.rawSlot(), () -> Minecraft.getInstance().gui.setScreen(screen));
    }

    /** Bridge used only by the Minecraft.setScreen field-write redirect. */
    public static void bindScreen(@Nullable Screen screen) {
        LocalClient active = current();
        active.ui().setScreen(screen);
        Screens.onSlotScreenChanged(Minecraft.getInstance(), active.slotId(), screen);
    }

    /** Bridge used by the 26.2 Gui.screen field-write transformer. */
    public static void bindScreen(Gui gui, @Nullable Screen screen) {
        LocalClient active = currentOrNull();
        if (active == null) {
            ((GuiRawScreenAccessor)gui).splitTest$setRawScreen(screen);
            LocalClient primary = client(0);
            primary.ui().setScreen(screen);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                Screens.onSlotScreenChanged(minecraft, primary.slotId(), screen);
            }
            return;
        }
        bindScreen(screen);
    }

    public static void setMenu(@Nullable AbstractContainerMenu menu) {
        current().ui().setMenu(menu);
    }

    public static void setMenu(int clientId, @Nullable AbstractContainerMenu menu) {
        client(clientId).ui().setMenu(menu);
    }

    public static void clearWorldBinding() {
        current().rawSlot().clearWorldBinding();
    }

    public static void clearWorldBinding(int clientId) {
        client(clientId).rawSlot().clearWorldBinding();
    }

    @Nullable
    private static Entity liveEntityOrNull(@Nullable Entity entity) {
        return entity != null && !entity.isRemoved() ? entity : null;
    }

    static LocalClient client(int clientId) {
        return new LocalClient(LocalPlayers.INSTANCE.slots().slot(clientId));
    }

}
