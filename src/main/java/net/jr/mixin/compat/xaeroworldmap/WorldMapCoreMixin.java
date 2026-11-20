package net.jr.mixin.compat.xaeroworldmap;

import net.jr.client.compat.xaero.XaeroWorldMapRuntime;
import net.jr.client.runtime.context.InstanceResolver;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.core.XaeroWorldMapCore;

@Mixin(value = XaeroWorldMapCore.class, remap = false)
abstract class WorldMapCoreMixin {
    @WrapMethod(method = "onPlayNetHandler")
    private static void javaReforged$bindSessionForJoiningPlayer(
        ClientPacketListener listener,
        ClientboundLoginPacket packet,
        Operation<Void> original
    ) {
        InstanceResolver.requireListener(listener).slot().runForSlot(() -> {
            try (XaeroWorldMapRuntime.CoreSessionScope ignored =
                     XaeroWorldMapRuntime.bindCoreSessionForJoin(listener)) {
                original.call(listener, packet);
            }
        });
    }

    @WrapMethod(method = "onPlayNetHandlerCleanup")
    private static void javaReforged$bindSessionForLeavingPlayer(
        ClientPacketListener listener,
        Operation<Void> original
    ) {
        InstanceResolver.requireListener(listener).slot().runForSlot(() -> {
            try (XaeroWorldMapRuntime.CoreSessionScope ignored =
                     XaeroWorldMapRuntime.bindCoreSessionForCleanup(listener)) {
                original.call(listener);
            }
        });
    }

    @Inject(method = "onPlayNetHandler", at = @At("RETURN"))
    private static void javaReforged$registerSession(
        ClientPacketListener listener,
        ClientboundLoginPacket packet,
        CallbackInfo ci
    ) {
        XaeroWorldMapRuntime.register(listener);
    }

    @Inject(method = "onPlayNetHandlerCleanup", at = @At("RETURN"))
    private static void javaReforged$removeCleanSession(
        ClientPacketListener listener,
        CallbackInfo ci
    ) {
        XaeroWorldMapRuntime.unregister(listener);
    }
}
