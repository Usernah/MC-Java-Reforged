package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.UUID;
import net.jr.client.runtime.network.ClientPacketHandler;
import net.jr.client.runtime.network.PacketExecutionRouter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
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
    private void javaReforged$handleLoginForListenerClient(ClientboundLoginPacket packet, Operation<Void> original) {
        this.javaReforged$runPacketForListener(() -> original.call(packet));
    }

    @WrapMethod(method = "handleOpenScreen")
    private void javaReforged$openScreenForListenerClient(ClientboundOpenScreenPacket packet, Operation<Void> original) {
        this.javaReforged$runPacketForListener(() -> original.call(packet));
    }

    @WrapMethod(method = "handlePlayerCombatKill")
    private void javaReforged$openDeathScreenForListenerClient(ClientboundPlayerCombatKillPacket packet, Operation<Void> original) {
        this.javaReforged$runPacketForListener(() -> original.call(packet));
    }

    @WrapMethod(method = "handleRespawn")
    private void javaReforged$handleRespawnForListenerClient(ClientboundRespawnPacket packet, Operation<Void> original) {
        this.javaReforged$runPacketForListener(() -> original.call(packet));
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void javaReforged$bindConnectionOnLogin(CallbackInfo ci) {
        ClientPacketHandler.bindConnection((ClientPacketListener) (Object) this);
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void javaReforged$bindConnectionOnRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ClientPacketHandler.bindConnection((ClientPacketListener) (Object) this);
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void javaReforged$closeLocalDeathScreenAfterRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ClientPacketHandler.closeDeathScreenAfterRespawn(Minecraft.getInstance());
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void javaReforged$clearSlotLevel(CallbackInfo ci) {
        ClientPacketHandler.clearLevel((ClientPacketListener) (Object) this);
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;setUUID(Ljava/util/UUID;)V")
    )
    private void javaReforged$skipIntegratedServerOwnerUuidForSecondary(IntegratedServer server, UUID uuid) {
        ClientPacketHandler.setIntegratedServerOwner(server, uuid);
    }

    @Redirect(
        method = "handleLogin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getConnection()Lnet/minecraft/client/multiplayer/ClientPacketListener;")
    )
    private ClientPacketListener javaReforged$useCurrentListenerForSecondaryLogin(Minecraft minecraft) {
        return ClientPacketHandler.loginListener(minecraft, (ClientPacketListener) (Object) this);
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void javaReforged$markInitialPositionSynchronized(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        ClientPacketHandler.markInitialPositionSynchronized((ClientPacketListener) (Object) this);
    }

    private void javaReforged$runPacketForListener(Runnable action) {
        PacketExecutionRouter.runForListener((ClientPacketListener) (Object) this, action);
    }
}
