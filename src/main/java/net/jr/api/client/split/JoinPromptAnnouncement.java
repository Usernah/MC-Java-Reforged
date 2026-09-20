package net.jr.api.client.split;

/** Announces presentation changes for the global local-player join prompt. */
public sealed interface JoinPromptAnnouncement {
    record Show(long deviceId, int playerNumber) implements JoinPromptAnnouncement {
        public Show {
            if (playerNumber < 2 || playerNumber > 4) {
                throw new IllegalArgumentException("Join prompt player number must be between 2 and 4");
            }
        }
    }

    record Hide() implements JoinPromptAnnouncement {
    }
}
