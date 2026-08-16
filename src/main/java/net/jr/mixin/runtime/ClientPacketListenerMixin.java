package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.UUID;

import net.jr.client.runtime.context.ActiveClientSlot;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @WrapMethod(method = "handleLogin")
    private void splitTest$handleLoginForListenerClient(
        ClientboundLoginPacket packet,
        Operation<Void> original
    ) {
        this.splitTest$runPacketForListener(() -> original.call(packet));
    }

    @WrapMethod(method = "handleOpenScreen")
    private void splitTest$openScreenForListenerClient(
        ClientboundOpenScreenPacket packet,
        Operation<Void> original
    ) {
        this.splitTest$runPacketForListener(() -> original.call(packet));
    }

    @WrapMethod(method = "handlePlayerCombatKill")
    private void splitTest$openDeathScreenForListenerClient(
        ClientboundPlayerCombatKillPacket packet,
        Operation<Void> original
    ) {
        this.splitTest$runPacketForListener(() -> original.call(packet));
    }

    @WrapMethod(method = "handleRespawn")
    private void splitTest$handleRespawnForListenerClient(
        ClientboundRespawnPacket packet,
        Operation<Void> original
    ) {
        this.splitTest$runPacketForListener(() -> original.call(packet));
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void splitTest$bindConnectionOnLogin(CallbackInfo ci) {
        this.splitTest$bindCurrentConnectionToActiveSlot();
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void splitTest$bindConnectionOnRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        this.splitTest$bindCurrentConnectionToActiveSlot();
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void splitTest$closeLocalDeathScreenAfterRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Screen screen = LocalClientAcces.screen();
        if (screen instanceof DeathScreen || screen instanceof DeathScreen.TitleConfirmScreen) {
            LocalClientAcces.setScreen(null);
        }
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void splitTest$clearSlotLevel(CallbackInfo ci) {
        ClientRuntime.INSTANCE.sessions().onClientLevelCleared((ClientPacketListener)(Object)this, ClientRuntime.INSTANCE);
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;setUUID(Ljava/util/UUID;)V")
    )
    private void splitTest$skipIntegratedServerOwnerUuidForSecondary(IntegratedServer server, UUID uuid) {
        Integer slotId = ActiveClientSlot.idOrNull();
        if (slotId == null || slotId == 0) {
            server.setUUID(uuid);
        }
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getConnection()Lnet/minecraft/client/multiplayer/ClientPacketListener;")
    )
    private ClientPacketListener splitTest$useCurrentListenerForSecondaryLogin(Minecraft minecraft) {
        Integer slotId = ActiveClientSlot.idOrNull();
        if (slotId != null && slotId != 0) {
            return (ClientPacketListener)(Object)this;
        }
        return minecraft.getConnection();
    }

    private void splitTest$bindCurrentConnectionToActiveSlot() {
        Connection connection = ((ClientPacketListener)(Object)this).getConnection();
        if (connection == null) {
            throw new IllegalStateException("ClientPacketListener has no connection");
        }
        Integer mappedSlotId = ClientRuntime.INSTANCE.connections().slotOrNull(connection);
        Integer activeSlotId = ActiveClientSlot.idOrNull();
        int slotId = mappedSlotId != null
            ? mappedSlotId
            : activeSlotId != null
                ? activeSlotId
                : ClientRuntime.INSTANCE.slotForClientPacketListener((ClientPacketListener)(Object)this);
        ClientRuntime.INSTANCE.connections().bind(connection, slotId);
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void splitTest$markInitialPositionSynchronized(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        ClientRuntime.INSTANCE.sessions().onInitialPositionSynchronized(
            (ClientPacketListener)(Object)this,
            ClientRuntime.INSTANCE
        );
    }

    private void splitTest$runPacketForListener(Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            action.run();
            return;
        }
        LocalClientExecution.runForListener((ClientPacketListener)(Object)this, action);
    }
}
