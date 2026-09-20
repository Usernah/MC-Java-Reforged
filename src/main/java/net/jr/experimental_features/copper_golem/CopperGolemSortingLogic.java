package net.jr.experimental_features.copper_golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.jspecify.annotations.Nullable;

public final class CopperGolemSortingLogic {

    public static final long REJECTED_ITEM_RETRY_DELAY_TICKS = 100L;

    public static final TagKey<Item> MINERAL_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            "java_reforged",
                            "copper_golem_family/mineral"
                    )
            );

    private static final double FAMILY_PRIORITY = 10_000.0D;
    private static final double EMPTY_PRIORITY = 20_000.0D;
    private static final double UNKNOWN_PRIORITY = 30_000.0D;

    private CopperGolemSortingLogic() {
    }

    public static double scoreDistance(
            PathfinderMob body,
            Level level,
            TransportItemsBetweenContainers.TransportItemTarget candidate,
            double vanillaDistance
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return vanillaDistance;
        }

        ItemStack carried = body.getMainHandItem();

        if (carried.isEmpty()) {
            return vanillaDistance;
        }

        BlockPos candidatePos =
                CopperGolemContainerMemory.normalizeChestPos(
                        level,
                        candidate.pos()
                );

        if (access.javaReforged$isReturningToSource()) {
            GlobalPos source =
                    access.javaReforged$getTransportSource();

            if (source == null
                    || !source.dimension().equals(level.dimension())) {
                return Double.MAX_VALUE;
            }

            BlockPos sourcePos =
                    CopperGolemContainerMemory.normalizeChestPos(
                            level,
                            source.pos()
                    );

            return sourcePos.equals(candidatePos)
                    ? vanillaDistance
                    : Double.MAX_VALUE;
        }

        Container container = candidate.container();

        if (containsSameItem(container, carried)
                && canAccept(container, carried)) {
            return vanillaDistance;
        }

        CopperGolemFamily carriedFamily =
                CopperGolemFamilyClassifier.classify(carried);

        CopperGolemContainerEntry remembered =
                access
                        .javaReforged$getContainerMemory()
                        .getUsable(level, candidate.pos());

        if (remembered != null
                && carriedFamily != CopperGolemFamily.UNKNOWN
                && remembered.family() == carriedFamily
                && canAccept(container, carried)) {
            return FAMILY_PRIORITY + vanillaDistance;
        }

        if (container.isEmpty()) {
            return EMPTY_PRIORITY + vanillaDistance;
        }

        if (remembered == null) {
            return UNKNOWN_PRIORITY + vanillaDistance;
        }

        return Double.MAX_VALUE;
    }

    public static boolean canPlaceByFamily(
            PathfinderMob body,
            Container container
    ) {
        ItemStack carried = body.getMainHandItem();

        CopperGolemFamily carriedFamily =
                CopperGolemFamilyClassifier.classify(carried);

        if (carriedFamily == CopperGolemFamily.UNKNOWN) {
            return false;
        }

        CopperGolemFamilyClassifier.Result actual =
                CopperGolemFamilyClassifier.classify(container);

        return actual.family() == carriedFamily
                && canAccept(container, carried);
    }

    public static boolean containsSameItem(
            Container container,
            ItemStack carried
    ) {
        for (ItemStack stack : container) {
            if (ItemStack.isSameItem(stack, carried)) {
                return true;
            }
        }

        return false;
    }

    public static boolean canAccept(
            Container container,
            ItemStack carried
    ) {
        for (ItemStack stack : container) {
            if (stack.isEmpty()) {
                return true;
            }

            if (ItemStack.isSameItemSameComponents(stack, carried)
                    && stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack takeItem(
            Container container,
            @Nullable Item rejectedItem
    ) {
        if (rejectedItem != null) {
            int slot = 0;

            for (ItemStack stack : container) {
                if (!stack.isEmpty()
                        && stack.getItem() != rejectedItem) {
                    int count =
                            Math.min(stack.getCount(), 16);

                    return container.removeItem(
                            slot,
                            count
                    );
                }

                slot++;
            }

            return ItemStack.EMPTY;
        }

        int slot = 0;

        for (ItemStack stack : container) {
            if (!stack.isEmpty()) {
                int count =
                        Math.min(stack.getCount(), 16);

                return container.removeItem(
                        slot,
                        count
                );
            }

            slot++;
        }

        return ItemStack.EMPTY;
    }
}
