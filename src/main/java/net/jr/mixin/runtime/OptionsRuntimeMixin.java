package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.CameraType;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsRuntimeMixin {
    @Shadow
    private CameraType cameraType;

    @Shadow
    @Final
    private OptionInstance<Integer> fov;

    @Inject(
            method = "getCameraType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotCameraType(
            CallbackInfoReturnable<CameraType> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().cameraType()
        );
    }

    @Inject(
            method = "setCameraType",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$setSlotCameraType(
            CameraType cameraType,
            CallbackInfo ci
    ) {
        int slotId = this.splitTest$slotId();

        ClientRuntime.INSTANCE
                .slots()
                .slot(slotId)
                .optionsState()
                .setCameraType(cameraType);

        /*
         * Keep Vanilla's raw field synchronized with the primary
         * because code running without a slot belongs to slot 0.
         */
        if (slotId == 0) {
            this.cameraType = cameraType;
        }

        ci.cancel();
    }

    @Inject(
            method = "fov",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$getSlotFov(
            CallbackInfoReturnable<OptionInstance<Integer>> cir
    ) {
        cir.setReturnValue(
                this.splitTest$optionsState().fov()
        );
    }

    /*
     * Vanilla loads its persisted FOV into its private OptionInstance.
     * Seed every stable slot from that value so joining players start
     * with the user's normal FOV instead of forcibly starting at 70.
     */
    @Inject(
            method = "load",
            at = @At("TAIL")
    )
    private void splitTest$initializeLocalVisualOptions(
            CallbackInfo ci
    ) {
        CameraType initialCameraType = this.cameraType;
        int initialFov = this.fov.get();

        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().all()) {
            slot.optionsState().initializeVisualOptions(
                    initialCameraType,
                    initialFov
            );
        }
    }

    /*
     * Slot 0 remains the persisted Vanilla options owner for now.
     * Secondary local-player options are runtime-local until profiles
     * have their own persistence.
     */
    @Inject(
            method = "save",
            at = @At("HEAD")
    )
    private void splitTest$syncPrimaryVisualOptionsBeforeSave(
            CallbackInfo ci
    ) {
        OptionsState primary =
                ClientRuntime.INSTANCE.slots().primary().optionsState();

        this.cameraType = primary.cameraType();
        this.fov.set(primary.fov().get());
    }

    @Unique
    private OptionsState splitTest$optionsState() {
        return ClientRuntime.INSTANCE
                .slots()
                .slot(this.splitTest$slotId())
                .optionsState();
    }

    @Unique
    private int splitTest$slotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }
}