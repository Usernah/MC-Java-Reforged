package net.jr.mixin.experimental_features.copper_golem;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.jr.experimental_features.copper_golem.CopperGolemContainerMemory;
import net.jr.experimental_features.copper_golem.CopperGolemMemoryAccess;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolem.class)
public abstract class CopperGolemMemoryMixin
        implements CopperGolemMemoryAccess {

    @Unique
    private CopperGolemContainerMemory javaReforged$containerMemory;

    @Unique
    private @Nullable GlobalPos javaReforged$transportSource;

    @Unique
    private boolean javaReforged$returningToSource;

    @Unique
    private @Nullable Item javaReforged$lastRejectedItem;

    @Unique
    private long javaReforged$retryAfterGameTime;

    @Override
    public CopperGolemContainerMemory javaReforged$getContainerMemory() {
        if (this.javaReforged$containerMemory == null) {
            this.javaReforged$containerMemory =
                    new CopperGolemContainerMemory();
        }

        return this.javaReforged$containerMemory;
    }

    @Override
    public @Nullable GlobalPos javaReforged$getTransportSource() {
        return this.javaReforged$transportSource;
    }

    @Override
    public void javaReforged$setTransportSource(
            @Nullable GlobalPos source
    ) {
        this.javaReforged$transportSource = source;
    }

    @Override
    public boolean javaReforged$isReturningToSource() {
        return this.javaReforged$returningToSource;
    }

    @Override
    public void javaReforged$setReturningToSource(
            boolean returning
    ) {
        this.javaReforged$returningToSource = returning;
    }

    @Override
    public @Nullable Item javaReforged$getLastRejectedItem() {
        return this.javaReforged$lastRejectedItem;
    }

    @Override
    public void javaReforged$setLastRejectedItem(
            @Nullable Item item
    ) {
        this.javaReforged$lastRejectedItem = item;
    }

    @Override
    public long javaReforged$getRetryAfterGameTime() {
        return this.javaReforged$retryAfterGameTime;
    }

    @Override
    public void javaReforged$setRetryAfterGameTime(long gameTime) {
        this.javaReforged$retryAfterGameTime = gameTime;
    }

    @Inject(
            method = "addAdditionalSaveData",
            at = @At("TAIL")
    )
    private void javaReforged$saveSortingMemory(
            ValueOutput output,
            CallbackInfo ci
    ) {
        ValueOutput state =
                output.child("java_reforged_copper_golem");

        this.javaReforged$getContainerMemory()
                .save(state);

        state.storeNullable(
                "transport_source",
                GlobalPos.CODEC,
                this.javaReforged$transportSource
        );

        state.putBoolean(
                "returning_to_source",
                this.javaReforged$returningToSource
        );

        state.putLong(
                "retry_after",
                this.javaReforged$retryAfterGameTime
        );

        if (this.javaReforged$lastRejectedItem != null) {
            Identifier id =
                    BuiltInRegistries.ITEM.getKey(
                            this.javaReforged$lastRejectedItem
                    );

            if (id != null) {
                state.putString(
                        "last_rejected_item",
                        id.toString()
                );
            }
        }
    }

    @Inject(
            method = "readAdditionalSaveData",
            at = @At("TAIL")
    )
    private void javaReforged$loadSortingMemory(
            ValueInput input,
            CallbackInfo ci
    ) {
        ValueInput state =
                input.childOrEmpty(
                        "java_reforged_copper_golem"
                );

        this.javaReforged$getContainerMemory()
                .load(state);

        this.javaReforged$transportSource =
                state.read(
                        "transport_source",
                        GlobalPos.CODEC
                ).orElse(null);

        this.javaReforged$returningToSource =
                state.getBooleanOr(
                        "returning_to_source",
                        false
                );

        this.javaReforged$retryAfterGameTime =
                state.getLongOr(
                        "retry_after",
                        0L
                );

        this.javaReforged$lastRejectedItem = null;

        String rejectedId =
                state.getStringOr(
                        "last_rejected_item",
                        ""
                );

        if (!rejectedId.isEmpty()) {
            Identifier identifier =
                    Identifier.tryParse(rejectedId);

            if (identifier != null) {
                this.javaReforged$lastRejectedItem =
                        BuiltInRegistries.ITEM
                                .getOptional(identifier)
                                .orElse(null);
            }
        }
    }
}
