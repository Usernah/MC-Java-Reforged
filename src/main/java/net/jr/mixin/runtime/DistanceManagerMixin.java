package net.jr.mixin.runtime;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.concurrent.Executor;
import net.jr.server.chunk.LegacyChunkAdmissionLimiter;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThrottlingChunkTaskDispatcher;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements LegacyChunkAdmissionLimiter.NativeTicketBridge {
    @Shadow @Final private ThrottlingChunkTaskDispatcher ticketDispatcher;
    @Shadow @Final private TicketStorage ticketStorage;
    @Shadow @Final private LongSet ticketsToRelease;
    @Shadow @Final private Executor mainThreadExecutor;
    @Shadow private int simulationDistance;

    @Inject(method = "addPlayer", at = @At("HEAD"))
    private void javaReforged$addLegacyPlayerFootprint(SectionPos position, ServerPlayer player, CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.addPlayer(this, position, player);
    }

    @Inject(method = "removePlayer", at = @At("HEAD"))
    private void javaReforged$deferLegacyPlayerRemoval(SectionPos position, ServerPlayer player, CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.removePlayer(this, position, player);
    }

    @Inject(method = "updatePlayerTickets", at = @At("TAIL"))
    private void javaReforged$updateLegacyViewDistance(int viewDistance, CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.updateViewDistance(this, viewDistance);
    }

    @Inject(method = "updateSimulationDistance", at = @At("TAIL"))
    private void javaReforged$updateManagedSimulationDistance(int simulationDistance, CallbackInfo callback) {
        LegacyChunkAdmissionLimiter.updateSimulationDistance(this, simulationDistance);
    }

    @Redirect(
        method = "addPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/DistanceManager$PlayerTicketTracker;update(JIZ)V"
        )
    )
    private void javaReforged$replaceVanillaPlayerTicketAdd(@Coerce Object tracker, long chunkPos, int level, boolean decrease) {
        LegacyChunkAdmissionLimiter.suppressVanillaPlayerTicketSource();
    }

    @Redirect(
        method = "removePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/DistanceManager$PlayerTicketTracker;update(JIZ)V"
        )
    )
    private void javaReforged$replaceVanillaPlayerTicketRemove(@Coerce Object tracker, long chunkPos, int level, boolean decrease) {
        LegacyChunkAdmissionLimiter.suppressVanillaPlayerTicketSource();
    }

    /**
     * Vanilla inserts this source directly on every player movement.  Its level
     * expands by the simulation radius, bypassing PlayerTicketTracker entirely.
     * The limiter supplies exact simulation tickets only after their matching
     * player-loading chunks have been admitted.
     */
    @Redirect(
        method = "addPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/TicketStorage;addTicket(Lnet/minecraft/server/level/Ticket;Lnet/minecraft/world/level/ChunkPos;)V"
        )
    )
    private void javaReforged$replaceVanillaSimulationTicketAdd(TicketStorage storage, Ticket ticket, net.minecraft.world.level.ChunkPos chunkPos) {
    }

    @Redirect(
        method = "removePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/TicketStorage;removeTicket(Lnet/minecraft/server/level/Ticket;Lnet/minecraft/world/level/ChunkPos;)V"
        )
    )
    private void javaReforged$replaceVanillaSimulationTicketRemove(TicketStorage storage, Ticket ticket, net.minecraft.world.level.ChunkPos chunkPos) {
    }

    @Redirect(
        method = "updateSimulationDistance",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/TicketStorage;replaceTicketLevelOfType(ILnet/minecraft/server/level/TicketType;)V"
        )
    )
    private void javaReforged$preserveManagedSimulationTicketLevels(TicketStorage storage, int level, TicketType type) {
    }

    @Override
    public DistanceManager javaReforged$distanceManager() {
        return (DistanceManager) (Object) this;
    }

    @Override
    public void javaReforged$submitPlayerLoadingTicket(long chunkPos, int priority) {
        Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING));
        this.ticketDispatcher.submit(
            () -> this.mainThreadExecutor.execute(() -> {
                if (LegacyChunkAdmissionLimiter.claimSubmittedTicket(this, chunkPos)) {
                    this.ticketStorage.addTicket(chunkPos, ticket);
                    this.ticketsToRelease.add(chunkPos);
                    LegacyChunkAdmissionLimiter.onPlayerLoadingTicketInserted(this, chunkPos);
                } else {
                    this.ticketDispatcher.release(
                        chunkPos,
                        () -> LegacyChunkAdmissionLimiter.onNativeTicketReleased(this, chunkPos),
                        true
                    );
                }
            }),
            chunkPos,
            () -> priority
        );
    }

    @Override
    public void javaReforged$releasePlayerLoadingTicket(long chunkPos) {
        Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING));
        this.ticketDispatcher.release(
            chunkPos,
            () -> this.mainThreadExecutor.execute(() -> {
                this.ticketStorage.removeTicket(chunkPos, ticket);
                LegacyChunkAdmissionLimiter.onNativeTicketReleased(this, chunkPos);
            }),
            true
        );
    }

    @Override
    public int javaReforged$simulationDistance() {
        return this.simulationDistance;
    }

    @Override
    public void javaReforged$addManagedSimulationTicket(long chunkPos) {
        this.ticketStorage.addTicket(
            chunkPos,
            new Ticket(TicketType.PLAYER_SIMULATION, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING))
        );
    }

    @Override
    public void javaReforged$removeManagedSimulationTicket(long chunkPos) {
        this.ticketStorage.removeTicket(
            chunkPos,
            new Ticket(TicketType.PLAYER_SIMULATION, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING))
        );
    }

}
