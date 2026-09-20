package net.jr.experimental_features.copper_golem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CopperGolemFamilyClassifier {

    public static final float DOMINANT_FAMILY_THRESHOLD = 0.65F;
    private static final Map<CopperGolemFamily, TagKey<Item>> FAMILY_TAGS = createFamilyTags();
    private static final List<TagKey<Item>> MINERAL_TAGS = List.of(
            Tags.Items.ORES,
            Tags.Items.RAW_MATERIALS,
            Tags.Items.INGOTS,
            Tags.Items.NUGGETS,
            Tags.Items.GEMS,
            Tags.Items.DUSTS
    );
    private static final List<TagKey<Item>> MATERIAL_TAGS = List.of(
            Tags.Items.BONES,
            Tags.Items.BRICKS,
            Tags.Items.CLUMPS,
            Tags.Items.CROPS,
            Tags.Items.DYES,
            Tags.Items.ENDER_PEARLS,
            Tags.Items.FEATHERS,
            Tags.Items.FERTILIZERS,
            Tags.Items.GUNPOWDERS,
            Tags.Items.LEATHERS,
            Tags.Items.RODS,
            Tags.Items.ROPES,
            Tags.Items.SEEDS,
            Tags.Items.SLIME_BALLS,
            Tags.Items.STRINGS
    );

    private CopperGolemFamilyClassifier() {
    }

    public static CopperGolemFamily classify(ItemStack stack) {
        if (stack.isEmpty()) {
            return CopperGolemFamily.UNKNOWN;
        }

        for (Map.Entry<CopperGolemFamily, TagKey<Item>> entry : FAMILY_TAGS.entrySet()) {
            if (stack.is(entry.getValue())) {
                return entry.getKey();
            }
        }

        if (stack.has(DataComponents.FOOD)) {
            return CopperGolemFamily.FOOD;
        }

        if (MINERAL_TAGS.stream().anyMatch(stack::is)) {
            return CopperGolemFamily.MINERAL;
        }

        if (stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.EQUIPPABLE)
                || stack.has(DataComponents.BLOCKS_ATTACKS)
                || stack.has(DataComponents.PIERCING_WEAPON)
                || stack.has(DataComponents.KINETIC_WEAPON)
                || stack.typeHolder().is(Tags.Items.TOOLS)
                || stack.typeHolder().is(Tags.Items.ARMORS)) {
            return CopperGolemFamily.EQUIPMENT;
        }

        if (stack.has(DataComponents.POTION_CONTENTS)) {
            return CopperGolemFamily.POTION;
        }

        if (stack.getItem() instanceof BlockItem) {
            return CopperGolemFamily.BLOCK;
        }

        if (MATERIAL_TAGS.stream().anyMatch(stack::is)) {
            return CopperGolemFamily.MATERIAL;
        }

        return CopperGolemFamily.MISCELLANEOUS;
    }

    public static Result classify(Container container) {
        int totalItems = 0;
        EnumMap<CopperGolemFamily, Integer> counts = new EnumMap<>(CopperGolemFamily.class);

        for (ItemStack stack : container) {
            if (stack.isEmpty()) {
                continue;
            }

            int count = stack.getCount();
            totalItems += count;

            CopperGolemFamily family = classify(stack);
            if (family != CopperGolemFamily.UNKNOWN
                    && family != CopperGolemFamily.MIXED) {
                counts.merge(family, count, Integer::sum);
            }
        }

        if (totalItems == 0 || counts.isEmpty()) {
            return new Result(
                    CopperGolemFamily.UNKNOWN,
                    0.0F
            );
        }

        int bestCount = 0;
        CopperGolemFamily bestFamily = CopperGolemFamily.UNKNOWN;
        for (Map.Entry<CopperGolemFamily, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestFamily = entry.getKey();
            }
        }

        float confidence = (float) bestCount / (float) totalItems;

        if (confidence < DOMINANT_FAMILY_THRESHOLD) {
            return new Result(
                    CopperGolemFamily.MIXED,
                    confidence
            );
        }

        return new Result(
                bestFamily,
                confidence
        );
    }

    private static Map<CopperGolemFamily, TagKey<Item>> createFamilyTags() {
        EnumMap<CopperGolemFamily, TagKey<Item>> tags = new EnumMap<>(CopperGolemFamily.class);
        for (CopperGolemFamily family : CopperGolemFamily.values()) {
            if (family != CopperGolemFamily.MIXED && family != CopperGolemFamily.UNKNOWN) {
                tags.put(
                        family,
                        TagKey.create(
                                Registries.ITEM,
                                Identifier.fromNamespaceAndPath(
                                        "java_reforged",
                                        "copper_golem_family/" + family.name().toLowerCase(java.util.Locale.ROOT)
                                )
                        )
                );
            }
        }
        return Map.copyOf(tags);
    }

    public record Result(
            CopperGolemFamily family,
            float confidence
    ) {
    }
}
