package net.jr.ClientRuntime.audio;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.jr.mixin.SSM.SoundEngineActiveSoundsAccessor;
import net.jr.mixin.SSM.SoundManagerEngineAccessor;
import net.jr.ClientRuntime.runtime.ActiveSlot;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SlotSoundRouting {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotSoundRouting.class);
    private static final boolean LOG_SOUND_ROUTING = Boolean.getBoolean("split.soundRoutingLog");
    private static final Set<SoundSource> SPATIAL_SOURCES = EnumSet.of(
        SoundSource.AMBIENT,
        SoundSource.BLOCKS,
        SoundSource.HOSTILE,
        SoundSource.NEUTRAL,
        SoundSource.PLAYERS,
        SoundSource.RECORDS,
        SoundSource.WEATHER
    );
    private static final Set<Integer> LOGGED_ROUTED_SLOTS = ConcurrentHashMap.newKeySet();

    private SlotSoundRouting() {
    }

    @Nullable
    public static SoundInstance route(@Nullable SoundInstance sound) {
        if (sound == null || sound instanceof SlotWrappedSoundInstance) {
            return sound;
        }

        Integer slotId = routableActiveSlotId();
        if (slotId == null) {
            return sound;
        }

        if (sound.getSource() == net.minecraft.sounds.SoundSource.MUSIC) {
            // Si es música, devolvemos el sonido original intacto para no romper el SoundManager
            return sound;
        }


        boolean transformSpatially = SPATIAL_SOURCES.contains(sound.getSource()) && !sound.isRelative();
        logFirstRoute(slotId, sound, transformSpatially);
        if (sound instanceof TickableSoundInstance tickableSound) {
            return new RoutedTickableSoundInstance(slotId, tickableSound, transformSpatially);
        }
        return new RoutedSoundInstance(slotId, sound, transformSpatially);
    }

    public static void stopExact(SoundManager soundManager, SoundInstance sound) {
        SoundEngine soundEngine = ((SoundManagerEngineAccessor)soundManager).splitTest$soundEngine();
        Integer slotId = currentSlotId();
        if (slotId == null) {
            soundEngine.stop(sound);
            return;
        }

        boolean stopped = false;
        for (SoundInstance activeSound : activeSounds(soundEngine)) {
            if (!belongsToSlot(activeSound, slotId)) {
                continue;
            }
            if (activeSound == sound || delegateOf(activeSound) == sound) {
                soundEngine.stop(activeSound);
                stopped = true;
            }
        }
        stopped |= removeQueuedExact(soundEngine, slotId, sound);
        if (!stopped && slotId == 0) {
            soundEngine.stop(sound);
        }
    }

    public static void stopMatching(
        SoundManager soundManager,
        @Nullable Identifier soundName,
        @Nullable SoundSource source
    ) {
        SoundEngine soundEngine = ((SoundManagerEngineAccessor)soundManager).splitTest$soundEngine();
        Integer slotId = currentSlotId();
        if (slotId == null) {
            soundEngine.stop(soundName, source);
            return;
        }

        for (SoundInstance activeSound : activeSounds(soundEngine)) {
            if (matches(activeSound, slotId, soundName, source)) {
                soundEngine.stop(activeSound);
            }
        }
        removeQueuedMatching(soundEngine, slotId, soundName, source);
    }

    public static void stopSlotSounds(SoundManager soundManager, int slotId) {
        SoundEngine soundEngine = ((SoundManagerEngineAccessor)soundManager).splitTest$soundEngine();
        for (SoundInstance activeSound : activeSounds(soundEngine)) {
            if (belongsToSlot(activeSound, slotId)) {
                soundEngine.stop(activeSound);
            }
        }
        removeQueuedMatching(soundEngine, slotId, null, null);
    }

    @Nullable
    private static Integer currentSlotId() {
        Integer activeSlotId = ActiveSlot.idOrNull();
        if (activeSlotId != null) {
            return activeSlotId;
        }
        Integer scheduledSlotId = ActiveSlot.scheduledIdOrNull();
        if (scheduledSlotId != null) {
            return scheduledSlotId;
        }
        return LocalPlayers.INSTANCE.slots().presentSlotCount() > 0 ? 0 : null;
    }

    @Nullable
    private static Integer routableActiveSlotId() {
        Integer activeSlotId = currentSlotId();
        if (activeSlotId == null) {
            return null;
        }
        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(activeSlotId);
        return slot.connected() && slot.visible() ? activeSlotId : null;
    }

    private static void logFirstRoute(int slotId, SoundInstance sound, boolean transformSpatially) {
        if (!LOG_SOUND_ROUTING) {
            return;
        }
        if (!LOGGED_ROUTED_SLOTS.add(slotId)) {
            return;
        }
        LOGGER.info(
            "Slot sound routing activated for slot {}: {} source={} relative={} spatialTransform={}",
            slotId,
            sound.getIdentifier(),
            sound.getSource(),
            sound.isRelative(),
            transformSpatially
        );
    }

    private static boolean belongsToSlot(SoundInstance sound, int slotId) {
        if (sound instanceof SlotWrappedSoundInstance wrappedSound) {
            return wrappedSound.splitTest$slotId() == slotId;
        }
        return slotId == 0;
    }

    @Nullable
    private static SoundInstance delegateOf(SoundInstance sound) {
        if (sound instanceof SlotWrappedSoundInstance wrappedSound) {
            return wrappedSound.splitTest$delegate();
        }
        return null;
    }

    private static List<SoundInstance> activeSounds(SoundEngine soundEngine) {
        return new ArrayList<>(((SoundEngineActiveSoundsAccessor)soundEngine).splitTest$instanceToChannel().keySet());
    }

    private static boolean removeQueuedExact(SoundEngine soundEngine, int slotId, SoundInstance sound) {
        boolean removed = false;
        SoundEngineActiveSoundsAccessor accessor = (SoundEngineActiveSoundsAccessor)soundEngine;
        Iterator<SoundInstance> queuedSounds = accessor.splitTest$queuedSounds().keySet().iterator();
        while (queuedSounds.hasNext()) {
            SoundInstance queuedSound = queuedSounds.next();
            if (belongsToSlot(queuedSound, slotId) && (queuedSound == sound || delegateOf(queuedSound) == sound)) {
                queuedSounds.remove();
                removed = true;
            }
        }

        Iterator<TickableSoundInstance> queuedTickableSounds = accessor.splitTest$queuedTickableSounds().iterator();
        while (queuedTickableSounds.hasNext()) {
            TickableSoundInstance queuedSound = queuedTickableSounds.next();
            if (belongsToSlot(queuedSound, slotId) && (queuedSound == sound || delegateOf(queuedSound) == sound)) {
                queuedTickableSounds.remove();
                removed = true;
            }
        }
        return removed;
    }

    private static void removeQueuedMatching(
        SoundEngine soundEngine,
        int slotId,
        @Nullable Identifier soundName,
        @Nullable SoundSource source
    ) {
        SoundEngineActiveSoundsAccessor accessor = (SoundEngineActiveSoundsAccessor)soundEngine;
        Iterator<SoundInstance> queuedSounds = accessor.splitTest$queuedSounds().keySet().iterator();
        while (queuedSounds.hasNext()) {
            if (matches(queuedSounds.next(), slotId, soundName, source)) {
                queuedSounds.remove();
            }
        }

        Iterator<TickableSoundInstance> queuedTickableSounds = accessor.splitTest$queuedTickableSounds().iterator();
        while (queuedTickableSounds.hasNext()) {
            if (matches(queuedTickableSounds.next(), slotId, soundName, source)) {
                queuedTickableSounds.remove();
            }
        }
    }

    private static boolean matches(
        SoundInstance sound,
        int slotId,
        @Nullable Identifier soundName,
        @Nullable SoundSource source
    ) {
        if (!belongsToSlot(sound, slotId)) {
            return false;
        }
        if (source != null && sound.getSource() != source) {
            return false;
        }
        return soundName == null || sound.getIdentifier().equals(soundName);
    }

    private static boolean isSlotPresent(int slotId) {
        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
        return slot.connected() && slot.visible();
    }

    private static boolean isSpatialSlotAlive(int slotId) {
        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
        return slot.connected()
            && slot.visible()
            && slot.renderState().level() != null
            && resolveCamera(slot) != null;
    }

    @Nullable
    private static Entity resolveCamera(PlayerSlot slot) {
        Entity cameraEntity = slot.renderState().cameraEntity();
        if (cameraEntity != null && !cameraEntity.isRemoved()) {
            return cameraEntity;
        }
        Entity player = slot.gameplayState().player();
        return player != null && !player.isRemoved() ? player : null;
    }

    private static Vec3 relativePosition(int slotId, SoundInstance sound) {
        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
        Entity camera = resolveCamera(slot);
        if (camera == null) {
            return Vec3.ZERO;
        }

        Vec3 cameraPos = camera.getEyePosition();
        Vec3 look = camera.getViewVector(1.0F).normalize();
        Vec3 up = camera.getUpVector(1.0F).normalize();
        Vec3 right = look.cross(up).normalize();
        Vec3 delta = new Vec3(sound.getX(), sound.getY(), sound.getZ()).subtract(cameraPos);
        if (look.lengthSqr() < 1.0E-6D || up.lengthSqr() < 1.0E-6D || right.lengthSqr() < 1.0E-6D) {
            return delta;
        }
        return new Vec3(delta.dot(right), delta.dot(up), -delta.dot(look));
    }

    private interface SlotWrappedSoundInstance {
        int splitTest$slotId();

        SoundInstance splitTest$delegate();
    }

    private abstract static class RoutedSoundBase implements SoundInstance, SlotWrappedSoundInstance {
        protected final int slotId;
        protected final SoundInstance delegate;
        private final boolean transformSpatially;

        private RoutedSoundBase(int slotId, SoundInstance delegate, boolean transformSpatially) {
            this.slotId = slotId;
            this.delegate = delegate;
            this.transformSpatially = transformSpatially;
        }

        @Override
        public int splitTest$slotId() {
            return this.slotId;
        }

        @Override
        public SoundInstance splitTest$delegate() {
            return this.delegate;
        }

        @Override
        public Identifier getIdentifier() {
            return this.delegate.getIdentifier();
        }

        @Override
        public net.minecraft.client.sounds.WeighedSoundEvents resolve(SoundManager soundManager) {
            return this.delegate.resolve(soundManager);
        }

        @Override
        public Sound getSound() {
            return this.delegate.getSound();
        }

        @Override
        public SoundSource getSource() {
            return this.delegate.getSource();
        }

        @Override
        public boolean isLooping() {
            return this.delegate.isLooping();
        }

        @Override
        public boolean isRelative() {
            return this.transformSpatially || this.delegate.isRelative();
        }

        @Override
        public int getDelay() {
            return this.delegate.getDelay();
        }

        @Override
        public float getVolume() {
            return this.delegate.getVolume();
        }

        @Override
        public float getPitch() {
            return this.delegate.getPitch();
        }

        @Override
        public double getX() {
            return this.transformSpatially ? relativePosition(this.slotId, this.delegate).x : this.delegate.getX();
        }

        @Override
        public double getY() {
            return this.transformSpatially ? relativePosition(this.slotId, this.delegate).y : this.delegate.getY();
        }

        @Override
        public double getZ() {
            return this.transformSpatially ? relativePosition(this.slotId, this.delegate).z : this.delegate.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return this.delegate.getAttenuation();
        }

        protected boolean splitTest$slotCanPlay() {
            return this.transformSpatially ? isSpatialSlotAlive(this.slotId) : isSlotPresent(this.slotId);
        }

        @Override
        public boolean canStartSilent() {
            return this.splitTest$slotCanPlay() && this.delegate.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return this.splitTest$slotCanPlay() && this.delegate.canPlaySound();
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return this.delegate.getStream(soundBuffers, sound, looping);
        }
    }

    private static final class RoutedSoundInstance extends RoutedSoundBase {
        private RoutedSoundInstance(int slotId, SoundInstance delegate, boolean transformSpatially) {
            super(slotId, delegate, transformSpatially);
        }
    }

    private static final class RoutedTickableSoundInstance extends RoutedSoundBase implements TickableSoundInstance {
        private final TickableSoundInstance delegate;

        private RoutedTickableSoundInstance(int slotId, TickableSoundInstance delegate, boolean transformSpatially) {
            super(slotId, delegate, transformSpatially);
            this.delegate = delegate;
        }

        @Override
        public boolean isStopped() {
            return !this.splitTest$slotCanPlay() || this.delegate.isStopped();
        }

        @Override
        public void tick() {
            this.delegate.tick();
        }
    }
}
