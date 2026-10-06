package net.jr.client.runtime.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.state.MinimapState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

public final class MinimapEngine {
    public static final int TEXTURE_SIZE = 110;

    private final MinimapSurfaceSampler sampler =
            new MinimapSurfaceSampler();

    private final DynamicTexture[] textures =
            new DynamicTexture[ClientSlots.MAX_SLOTS];

    private final Identifier[] textureIds =
            new Identifier[ClientSlots.MAX_SLOTS];

    private final int[] lastCenterX =
            new int[ClientSlots.MAX_SLOTS];

    private final int[] lastCenterZ =
            new int[ClientSlots.MAX_SLOTS];

    private final Object[] lastDimension =
            new Object[ClientSlots.MAX_SLOTS];

    private final boolean[] initialized =
            new boolean[ClientSlots.MAX_SLOTS];

    public void updateView(ClientInstance client) {
        MinimapState state = client.minimap();
        LocalPlayer player = client.player();

        if (player == null) {
            state.clearWorldBinding();
            return;
        }

        state.updateView(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.level().dimension(),
                player.getYRot()
        );

        updateTexture(client, (ClientLevel) player.level());
    }

    public Identifier texture(int slotId) {
        ensureTexture(slotId);
        return this.textureIds[slotId];
    }

    private void updateTexture(
            ClientInstance client,
            ClientLevel level
    ) {
        int slotId = client.slotId();

        int centerX = (int) Math.floor(client.minimap().x());
        int centerZ = (int) Math.floor(client.minimap().z());

        Object dimension = level.dimension();

        if (
                this.initialized[slotId]
                        && this.lastCenterX[slotId] == centerX
                        && this.lastCenterZ[slotId] == centerZ
                        && this.lastDimension[slotId] == dimension
        ) {
            return;
        }

        ensureTexture(slotId);

        DynamicTexture texture = this.textures[slotId];
        NativeImage pixels = texture.getPixels();

        int half = TEXTURE_SIZE / 2;

        double previousHeight = Double.NaN;

        for (int imageY = 0; imageY < TEXTURE_SIZE; imageY++) {
            int worldZ = centerZ + imageY - half;

            previousHeight = Double.NaN;

            for (int imageX = 0; imageX < TEXTURE_SIZE; imageX++) {
                int worldX = centerX + imageX - half;

                MinimapSurfaceSampler.Sample sample =
                        this.sampler.sample(level, worldX, worldZ);

                if (!sample.available()) {
                    pixels.setPixel(
                            imageX,
                            imageY,
                            0x00000000
                    );

                    continue;
                }

                MapColor.Brightness brightness =
                        brightnessFor(
                                sample.color(),
                                sample.height(),
                                previousHeight,
                                imageX,
                                imageY
                        );

                byte packed =
                        sample.color().getPackedId(brightness);

                int argb =
                        MapColor.getColorFromPackedId(packed);

                pixels.setPixel(
                        imageX,
                        imageY,
                        argb
                );

                previousHeight = sample.height();
            }
        }

        texture.upload();

        this.lastCenterX[slotId] = centerX;
        this.lastCenterZ[slotId] = centerZ;
        this.lastDimension[slotId] = dimension;
        this.initialized[slotId] = true;
    }

    private static MapColor.Brightness brightnessFor(
            MapColor color,
            int height,
            double previousHeight,
            int x,
            int y
    ) {
        if (color == MapColor.WATER) {
            return ((x + y) & 1) == 0
                    ? MapColor.Brightness.NORMAL
                    : MapColor.Brightness.LOW;
        }

        if (Double.isNaN(previousHeight)) {
            return MapColor.Brightness.NORMAL;
        }


        double difference =
                height - previousHeight;

        double shade =
                difference * 4.0 / 5.0
                        + (((x + y) & 1) - 0.5) * 0.4;

        if (shade > 0.6) {
            return MapColor.Brightness.HIGH;
        }

        if (shade < -0.6) {
            return MapColor.Brightness.LOW;
        }

        return MapColor.Brightness.NORMAL;
    }

    private void ensureTexture(int slotId) {
        if (this.textures[slotId] != null) {
            return;
        }

        Identifier id =
                Identifier.fromNamespaceAndPath(
                        "jr",
                        "minimap/slot_" + slotId
                );

        DynamicTexture texture =
                new DynamicTexture(
                        () -> "Java Reforged Minimap " + slotId,
                        TEXTURE_SIZE,
                        TEXTURE_SIZE,
                        true
                );

        Minecraft.getInstance()
                .getTextureManager()
                .register(id, texture);

        this.textureIds[slotId] = id;
        this.textures[slotId] = texture;
    }

    public double offsetX(ClientInstance client) {
        int slotId = client.slotId();

        if (!this.initialized[slotId]) {
            return 0.0;
        }

        return client.minimap().x() - this.lastCenterX[slotId];
    }

    public double offsetZ(ClientInstance client) {
        int slotId = client.slotId();

        if (!this.initialized[slotId]) {
            return 0.0;
        }

        return client.minimap().z() - this.lastCenterZ[slotId];
    }

}