package net.jr.network;

import net.jr.Java_reforged;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Identifies the block behind a vanilla menu opening without changing its menu type. */
public final class ContainerOriginNetwork {
    private ContainerOriginNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(OpenOrigin.TYPE, OpenOrigin.STREAM_CODEC);
    }

    public record OpenOrigin(int containerId, String blockId) implements CustomPacketPayload {
        public static final Type<OpenOrigin> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Java_reforged.MODID, "container_origin")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenOrigin> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenOrigin::containerId,
            ByteBufCodecs.STRING_UTF8, OpenOrigin::blockId,
            OpenOrigin::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
