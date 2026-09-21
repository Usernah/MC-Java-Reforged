package net.jr.api.client.split;

import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IJoinPromptSource;

/** Announces presentation changes for the global local-player join prompt. */
public sealed interface JoinPromptAnnouncement {
    record Show(IGamepad gamepad, int playerNumber) implements JoinPromptAnnouncement, IJoinPromptSource {
        public Show {
            if (playerNumber < 2 || playerNumber > 4) {
                throw new IllegalArgumentException("Join prompt player number must be between 2 and 4");
            }
        }
    }

    record Hide() implements JoinPromptAnnouncement {
    }
}
