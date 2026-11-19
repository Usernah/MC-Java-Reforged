package net.jr.experimental_features.minigame.battle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.jr.Java_reforged;
import net.jr.experimental_features.minigame.MapPoints;
import net.jr.experimental_features.minigame.PointGroup;
import net.jr.experimental_features.minigame.PointPos;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class BattleChestLoot {
    private static final String ROOT = "loot_table/battle/normal/";
    private static final int REFILL_CHEST_COUNT = 4;
    private static final double PLAYER_NEARBY_RADIUS_SQUARED = 16.0D * 16.0D;
    private static final int DEFAULT_REFILL_WEIGHT = 4;
    private static final int NEARBY_PLAYER_REFILL_WEIGHT = 1;

    private BattleChestLoot() {
    }

    public static void fillInitial(MinecraftServer server, ServerLevel level) {
        fillGroup(server, level, PointGroup.CHEST_CENTRAL, "start", true);
        fillGroup(server, level, PointGroup.CHEST_SECONDARY, "outer", true);

        fillGroup(server, level, PointGroup.CHEST_SPECIAL, "high_value", true);
    }

    public static List<BlockPos> refillRandom(MinecraftServer server, ServerLevel level) {
        List<ChestCandidate> candidates = refillCandidates(level);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockPos> refilled = new ArrayList<>();
        int selectionCount = Math.min(REFILL_CHEST_COUNT, candidates.size());
        for (int selection = 0; selection < selectionCount; selection++) {
            ChestCandidate candidate = removeWeightedCandidate(candidates, level, level.getRandom());
            BlockPos pos = candidate.pos();
            Container container = chestAt(level, pos);
            try {
                LootParams params = lootParams(level, pos);
                List<ItemStack> stacks;
                if (candidate.group() == PointGroup.CHEST_CENTRAL) {
                    LootTable update = decodeTable(server, readJson(server, "update_items.json"),
                        Identifier.fromNamespaceAndPath(Java_reforged.MODID, "battle/normal/update_items"));
                    stacks = new ArrayList<>();
                    for (int roll = 0; roll < 3; roll++) {
                        stacks.addAll(update.getRandomItems(params));
                    }
                } else {
                    String directory = candidate.group() == PointGroup.CHEST_SPECIAL ? "high_value" : "outer";
                    stacks = rollGroup(loadGroup(server, directory), params, level.getRandom());
                }
                if (placeStacks(container, stacks, level.getRandom())) {
                    refilled.add(pos);
                }
            } catch (RuntimeException | IOException exception) {
                Java_reforged.LOGGER.error("Unable to refill Battle chest at {}", pos, exception);
            }
        }
        return refilled;
    }

    private static List<ChestCandidate> refillCandidates(ServerLevel level) {
        List<ChestCandidate> candidates = new ArrayList<>();
        for (PointGroup group : List.of(PointGroup.CHEST_CENTRAL, PointGroup.CHEST_SECONDARY, PointGroup.CHEST_SPECIAL)) {
            for (PointPos point : MapPoints.INSTANCE.getPoints(group)) {
                BlockPos pos = blockPos(point);
                if (chestAt(level, pos) != null) {
                    candidates.add(new ChestCandidate(pos.immutable(), group));
                }
            }
        }
        return candidates;
    }

    private static ChestCandidate removeWeightedCandidate(
        List<ChestCandidate> candidates,
        ServerLevel level,
        RandomSource random
    ) {
        int totalWeight = 0;
        for (ChestCandidate candidate : candidates) {
            totalWeight += refillWeight(candidate.pos(), level);
        }

        int roll = random.nextInt(totalWeight);
        for (int index = 0; index < candidates.size(); index++) {
            ChestCandidate candidate = candidates.get(index);
            roll -= refillWeight(candidate.pos(), level);
            if (roll < 0) {
                return candidates.remove(index);
            }
        }
        return candidates.removeLast();
    }

    private static int refillWeight(BlockPos pos, ServerLevel level) {
        Vec3 center = Vec3.atCenterOf(pos);
        boolean playerNearby = level.players().stream()
            .filter(ServerPlayer::isAlive)
            .filter(player -> !player.isSpectator())
            .filter(player -> BattleRuntime.isParticipant(player.getUUID()))
            .anyMatch(player -> player.distanceToSqr(center) <= PLAYER_NEARBY_RADIUS_SQUARED);
        return playerNearby ? NEARBY_PLAYER_REFILL_WEIGHT : DEFAULT_REFILL_WEIGHT;
    }

    private static void fillGroup(MinecraftServer server, ServerLevel level, PointGroup pointGroup, String directory, boolean clear) {
        List<PointPos> points = MapPoints.INSTANCE.getPoints(pointGroup);
        if (points.isEmpty()) {
            return;
        }
        try {
            GroupLoot loot = loadGroup(server, directory);

            for (PointPos point : points) {
                BlockPos pos = blockPos(point);
                Container container = chestAt(level, pos);
                if (container == null) {
                    Java_reforged.LOGGER.warn("No Battle chest at {} for {}", pos, directory);
                    continue;
                }
                LootParams params = lootParams(level, pos);
                List<ItemStack> stacks = rollGroup(loot, params, level.getRandom());
                if (clear) {
                    if (container instanceof RandomizableContainerBlockEntity randomizable) {
                        randomizable.setLootTable(null);
                    }
                    container.clearContent();
                }
                placeStacks(container, stacks, level.getRandom());
            }
        } catch (RuntimeException | IOException exception) {
            Java_reforged.LOGGER.error("Unable to load Battle loot group {}", directory, exception);
        }
    }

    private static GroupLoot loadGroup(MinecraftServer server, String directory) throws IOException {
        List<BattleEntry> kits = readEntries(server, directory + "/kits.json");
        JsonObject extraRoot = readJson(server, directory + "/extra.json");
        JsonObject battle = extraRoot.getAsJsonObject("battle");
        List<BattleEntry> extras = decodeEntries(server, extraRoot);
        if (kits.isEmpty() || extras.isEmpty()) {
            throw new IllegalArgumentException("Battle loot group " + directory + " has no kits or extras");
        }
        return new GroupLoot(kits, extras, battle.get("target_score").getAsInt(),
            battle.get("allow_overflow").getAsBoolean());
    }

    private static List<ItemStack> rollGroup(GroupLoot loot, LootParams params, RandomSource random) {
        BattleEntry kit = pickWeighted(loot.kits(), random);
        List<ItemStack> stacks = new ArrayList<>(kit.table().getRandomItems(params));
        int score = kit.kpScore();
        int rolls = 0;
        while (score < loot.targetScore() && rolls++ < 64) {
            int remainingScore = loot.targetScore() - score;
            List<BattleEntry> eligible = loot.allowOverflow() ? loot.extras() : loot.extras().stream()
                .filter(entry -> entry.kpScore() <= remainingScore)
                .toList();
            if (eligible.isEmpty()) {
                break;
            }
            BattleEntry extra = eligible.get(random.nextInt(eligible.size()));
            stacks.addAll(extra.table().getRandomItems(params));
            score += extra.kpScore();
        }
        return stacks;
    }

    private static List<BattleEntry> readEntries(MinecraftServer server, String path) throws IOException {
        return decodeEntries(server, readJson(server, path));
    }

    private static List<BattleEntry> decodeEntries(MinecraftServer server, JsonObject root) {
        JsonArray entries = root.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        String group = root.getAsJsonObject("battle").get("group").getAsString();
        List<BattleEntry> result = new ArrayList<>(entries.size());
        for (JsonElement element : entries) {
            JsonObject option = element.getAsJsonObject();
            JsonObject battle = option.getAsJsonObject("battle");
            int weight = option.get("weight").getAsInt();
            int kpScore = battle.get("kp_score").getAsInt();
            String id = battle.get("id").getAsString();
            if (weight <= 0 || kpScore <= 0) {
                throw new IllegalArgumentException("Battle loot weight and kp_score must be positive");
            }
            Identifier tableId = Identifier.fromNamespaceAndPath(Java_reforged.MODID,
                "battle/normal/" + group + "/" + id);
            result.add(new BattleEntry(decodeTable(server, option.getAsJsonObject("value"), tableId), weight, kpScore));
        }
        return result;
    }

    private static JsonObject readJson(MinecraftServer server, String path) throws IOException {
        Identifier id = Identifier.fromNamespaceAndPath(Java_reforged.MODID, ROOT + path);
        var resource = server.getResourceManager().getResource(id)
            .orElseThrow(() -> new IOException("Missing Battle loot resource " + id));
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static LootTable decodeTable(MinecraftServer server, JsonObject json, Identifier tableId) {
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        LootTable table = LootTable.DIRECT_CODEC.parse(ops, json).getOrThrow();
        table.setLootTableId(tableId);
        return table;
    }

    private static BattleEntry pickWeighted(List<BattleEntry> entries, RandomSource random) {
        int total = entries.stream().mapToInt(BattleEntry::weight).sum();
        int roll = random.nextInt(total);
        for (BattleEntry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry;
            }
        }
        return entries.getLast();
    }

    private static LootParams lootParams(ServerLevel level, BlockPos pos) {
        return new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .create(LootContextParamSets.CHEST);
    }

    private static boolean placeStacks(Container container, List<ItemStack> stacks, RandomSource random) {
        List<Integer> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                emptySlots.add(slot);
            }
        }
        Collections.shuffle(emptySlots, new java.util.Random(random.nextLong()));
        int placed = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || emptySlots.isEmpty()) {
                continue;
            }
            container.setItem(emptySlots.removeLast(), stack.copy());
            placed++;
        }
        if (placed > 0) {
            container.setChanged();
        }
        return placed > 0;
    }

    private static Container chestAt(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ChestBlockEntity chest ? chest : null;
    }

    private static BlockPos blockPos(PointPos point) {
        return new BlockPos(point.X(), point.Y(), point.Z());
    }

    private record BattleEntry(LootTable table, int weight, int kpScore) {
    }

    private record ChestCandidate(BlockPos pos, PointGroup group) {
    }

    private record GroupLoot(List<BattleEntry> kits, List<BattleEntry> extras, int targetScore, boolean allowOverflow) {
    }
}
