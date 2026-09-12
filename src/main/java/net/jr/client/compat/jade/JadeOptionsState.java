package net.jr.client.compat.jade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.File;
import snownee.jade.Jade;
import snownee.jade.impl.config.WailaConfig;
import snownee.jade.util.JadeCodecs;
import snownee.jade.util.JsonConfig;

public final class JadeOptionsState {
    private static final Codec<WailaConfig> CODEC = WailaConfig.MAP_CODEC.codec();

    private final int slotId;
    private JsonConfig<WailaConfig> holder;

    public JadeOptionsState(int slotId) {
        this.slotId = slotId;
    }

    public WailaConfig config(WailaConfig primary) {
        if (this.slotId == 0) {
            return primary;
        }
        return this.holder(primary).get();
    }

    public boolean save() {
        if (this.slotId == 0) {
            return false;
        }
        if (this.holder != null) {
            this.holder.save();
        }
        return true;
    }

    public boolean invalidate() {
        if (this.slotId == 0) {
            return false;
        }
        if (this.holder != null) {
            this.holder.invalidate();
        }
        return true;
    }

    public boolean reset(WailaConfig primary) {
        if (this.slotId == 0) {
            return false;
        }

        JsonConfig<WailaConfig> holder = this.holder(primary);
        File file = holder.getFile();
        if (file.exists() && !file.delete()) {
            Jade.LOGGER.warn("Failed to reset Jade options for local player {}", this.slotId + 1);
            return true;
        }

        WailaConfig defaults = JadeCodecs.createFromEmptyMap(CODEC);
        defaults.fixData();
        holder.write(file, defaults, true);
        holder.get();
        return true;
    }

    private JsonConfig<WailaConfig> holder(WailaConfig primary) {
        if (this.holder == null) {
            WailaConfig defaults = copy(primary);
            this.holder = new JsonConfig<>(
                "jade/jade-player-" + (this.slotId + 1),
                CODEC,
                WailaConfig::fixData,
                () -> copy(defaults)
            );
        }
        return this.holder;
    }

    private static WailaConfig copy(WailaConfig source) {
        return CODEC.parse(
            JsonOps.INSTANCE,
            CODEC.encodeStart(JsonOps.INSTANCE, source).getOrThrow()
        ).getOrThrow();
    }
}
