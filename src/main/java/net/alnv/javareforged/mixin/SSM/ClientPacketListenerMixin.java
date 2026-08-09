package net.alnv.javareforged.mixin.SSM;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.UUID;

import net.alnv.javareforged.ClientRuntime.runtime.ActiveSlot;
import net.alnv.javareforged.ClientRuntime.runtime.Client;
import net.alnv.javareforged.ClientRuntime.runtime.ClientBoundary;
import net.alnv.javareforged.ClientRuntime.runtime.LocalPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @WrapMethod(method = "handleOpenScreen")
    private void splitTest$openScreenForListenerClient(
        ClientboundOpenScreenPacket packet,
        Operation<Void> original
    ) {
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        ClientBoundary.runForListener(listener, () -> original.call(packet));
    }

    @WrapMethod(method = "handlePlayerCombatKill")
    private void splitTest$openDeathScreenForListenerClient(
        ClientboundPlayerCombatKillPacket packet,
        Operation<Void> original
    ) {
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        ClientBoundary.runForListener(listener, () -> original.call(packet));
    }

    @WrapMethod(method = "handleRespawn")
    private void splitTest$handleRespawnForListenerClient(
        ClientboundRespawnPacket packet,
        Operation<Void> original
    ) {
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        ClientBoundary.runForListener(listener, () -> original.call(packet));
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
        Screen screen = Client.screen();
        if (screen instanceof DeathScreen || screen instanceof DeathScreen.TitleConfirmScreen) {
            Client.setScreen(null);
        }
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void splitTest$clearSlotLevel(CallbackInfo ci) {
        LocalPlayers.INSTANCE.sessions().onClientLevelCleared((ClientPacketListener)(Object)this, LocalPlayers.INSTANCE);
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;gameMode:Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$captureGameModeWrite(Minecraft minecraft, MultiPlayerGameMode gameMode) {
        LocalPlayers.INSTANCE.activeSlot().gameplayState().bindGameMode(gameMode);
        minecraft.gameMode = null;
    }

    @Redirect(
        method = {"handleLogin", "handleRespawn"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/player/LocalPlayer;", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$capturePlayerWrite(Minecraft minecraft, LocalPlayer player) {
        LocalPlayers.INSTANCE.activeSlot().gameplayState().bindPlayer(player);
        minecraft.player = null;
    }

    @Redirect(
        method = {"handleLogin", "handleRespawn"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;cameraEntity:Lnet/minecraft/world/entity/Entity;", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$captureCameraEntityWrite(Minecraft minecraft, Entity cameraEntity) {
        LocalPlayers.INSTANCE.activeSlot().renderState().bindCameraEntity(cameraEntity);
        minecraft.cameraEntity = null;
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;setUUID(Ljava/util/UUID;)V")
    )
    private void splitTest$skipIntegratedServerOwnerUuidForSecondary(IntegratedServer server, UUID uuid) {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId == null || slotId == 0) {
            server.setUUID(uuid);
        }
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getConnection()Lnet/minecraft/client/multiplayer/ClientPacketListener;")
    )
    private ClientPacketListener splitTest$useCurrentListenerForSecondaryLogin(Minecraft minecraft) {
        Integer slotId = ActiveSlot.idOrNull();
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
        Integer activeSlotId = ActiveSlot.idOrNull();
        int slotId = activeSlotId != null
            ? activeSlotId
            : LocalPlayers.INSTANCE.slotForClientPacketListener((ClientPacketListener)(Object)this);
        LocalPlayers.INSTANCE.connections().bind(connection, slotId);
    }
}
