package net.jr.experimental_features.copper_golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class CopperGolemContainerMemory {

    public static final int MAX_ENTRIES = 32;

    public static final long STALE_AFTER_TICKS = 6000L;

    private final Map<GlobalPos, CopperGolemContainerEntry> entries =
            new HashMap<>();

    public void observe(
            Level level,
            BlockPos pos,
            Container container
    ) {
        GlobalPos normalizedPos = GlobalPos.of(
                level.dimension(),
                normalizeChestPos(level, pos)
        );

        CopperGolemFamilyClassifier.Result classification =
                CopperGolemFamilyClassifier.classify(container);

        entries.put(
                normalizedPos,
                new CopperGolemContainerEntry(
                        normalizedPos,
                        classification.family(),
                        classification.confidence(),
                        level.getGameTime()
                )
        );

        trimToMaximumSize();
    }

    public @Nullable CopperGolemContainerEntry getUsable(
            Level level,
            BlockPos pos
    ) {
        GlobalPos normalizedPos = GlobalPos.of(
                level.dimension(),
                normalizeChestPos(level, pos)
        );

        CopperGolemContainerEntry entry = entries.get(normalizedPos);

        if (entry == null) {
            return null;
        }

        long age = level.getGameTime() - entry.lastSeenGameTime();

        if (age > STALE_AFTER_TICKS) {
            return null;
        }

        return entry;
    }

    public void clear() {
        entries.clear();
    }

    public void save(ValueOutput output) {
        ValueOutput.ValueOutputList list =
                output.childrenList("containers");

        for (CopperGolemContainerEntry entry : entries.values()) {
            ValueOutput child = list.addChild();

            child.store(
                    "pos",
                    GlobalPos.CODEC,
                    entry.pos()
            );

            child.putString(
                    "family",
                    entry.family().name()
            );

            child.putFloat(
                    "confidence",
                    entry.confidence()
            );

            child.putLong(
                    "last_seen",
                    entry.lastSeenGameTime()
            );
        }
    }

    public void load(ValueInput input) {
        entries.clear();

        for (ValueInput child : input.childrenListOrEmpty("containers")) {
            GlobalPos pos = child
                    .read("pos", GlobalPos.CODEC)
                    .orElse(null);

            if (pos == null) {
                continue;
            }

            String familyName =
                    child.getStringOr(
                            "family",
                            CopperGolemFamily.UNKNOWN.name()
                    );

            CopperGolemFamily family;

            try {
                family = CopperGolemFamily.valueOf(familyName);
            } catch (IllegalArgumentException ignored) {
                family = CopperGolemFamily.UNKNOWN;
            }

            float confidence =
                    child.getFloatOr("confidence", 0.0F);

            long lastSeen =
                    child.getLongOr("last_seen", 0L);

            entries.put(
                    pos,
                    new CopperGolemContainerEntry(
                            pos,
                            family,
                            confidence,
                            lastSeen
                    )
            );
        }

        trimToMaximumSize();
    }

    public static BlockPos normalizeChestPos(
            Level level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof ChestBlock)) {
            return pos.immutable();
        }

        ChestType type = state.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE) {
            return pos.immutable();
        }

        BlockPos connected =
                pos.relative(
                        ChestBlock.getConnectedDirection(state)
                );

        return Long.compare(
                pos.asLong(),
                connected.asLong()
        ) <= 0
                ? pos.immutable()
                : connected.immutable();
    }

    private void trimToMaximumSize() {
        while (entries.size() > MAX_ENTRIES) {
            GlobalPos oldestPos = null;
            long oldestTime = Long.MAX_VALUE;

            for (CopperGolemContainerEntry entry : entries.values()) {
                if (entry.lastSeenGameTime() < oldestTime) {
                    oldestTime = entry.lastSeenGameTime();
                    oldestPos = entry.pos();
                }
            }

            if (oldestPos == null) {
                break;
            }

            entries.remove(oldestPos);
        }
    }
}
