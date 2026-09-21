package net.jr.client.sound.music;

import java.util.Objects;
import net.minecraft.sounds.Music;
import org.jetbrains.annotations.Nullable;

/** The current owner of the music channel, independent of the track scheduler. */
public final class MusicPlaybackPolicy {
    public enum Mode {
        VANILLA,
        SILENT,
        OVERRIDE
    }

    public record Selection(Mode mode, @Nullable Music music) {
        public Selection {
            Objects.requireNonNull(mode);
            if ((mode == Mode.OVERRIDE) != (music != null)) {
                throw new IllegalArgumentException("An override must have music, and other modes must not");
            }
        }
    }

    public static final Selection VANILLA = new Selection(Mode.VANILLA, null);
    public static final Selection SILENT = new Selection(Mode.SILENT, null);

    private static volatile Selection current = VANILLA;

    private MusicPlaybackPolicy() {
    }

    public static Selection current() {
        return current;
    }

    public static void useVanilla() {
        current = VANILLA;
    }

    public static void silence() {
        current = SILENT;
    }

    public static void overrideWith(Music music) {
        current = new Selection(Mode.OVERRIDE, Objects.requireNonNull(music));
    }
}
