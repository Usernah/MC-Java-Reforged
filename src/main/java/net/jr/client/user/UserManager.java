package net.jr.client.user;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.util.JsonFileStore;
import net.minecraft.client.Minecraft;

public final class UserManager {
    private static final String FILE_NAME = "java_reforged_users.json";
    private static final String DEFAULT_ICON =
        "java_reforged:textures/system/users/default_user.png";
    private static final UserManager INSTANCE = new UserManager();

    private final Map<UUID, UserProfile> localUsers = new LinkedHashMap<>();
    private final Map<Integer, UUID> activeUsers = new HashMap<>();
    private boolean loaded;
    private Path file;

    private UserManager() {
    }

    public static UserManager get() {
        return INSTANCE;
    }

    public synchronized List<UserProfile> users() {
        this.ensureLoaded();
        UserProfile primary = this.minecraftUser();
        List<UserProfile> result = new ArrayList<>();
        result.add(primary);
        this.localUsers.values().stream()
            .filter(user -> !user.id().equals(primary.id()))
            .sorted(Comparator.comparingLong(UserProfile::lastUsedAt).reversed())
            .forEach(result::add);
        return List.copyOf(result);
    }

    public synchronized UserProfile minecraftUser() {
        Minecraft minecraft = Minecraft.getInstance();
        String name = minecraft.getUser().getName();
        UUID id = minecraft.getUser().getProfileId();
        long now = System.currentTimeMillis();
        UserProfile profile = new UserProfile(
            id,
            name,
            new UserIcon(DEFAULT_ICON),
            UserType.MINECRAFT,
            new UserOptions(),
            now,
            now
        );
        this.activeUsers.put(0, id);
        return profile;
    }

    public synchronized UserProfile createRandomTestUser() {
        this.ensureLoaded();
        while (true) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            String name = "DevUsserTest"
                + random.nextInt(100, 1000)
                + (char)('A' + random.nextInt(26));
            if (this.nameExists(name)) {
                continue;
            }
            long now = System.currentTimeMillis();
            UserProfile profile = new UserProfile(
                UUID.randomUUID(),
                name,
                new UserIcon(
                    "java_reforged:textures/system/users/" + name.toLowerCase(Locale.ROOT) + ".png"
                ),
                UserType.LOCAL,
                new UserOptions(),
                now,
                now
            );
            this.localUsers.put(profile.id(), profile);
            this.save();
            return profile;
        }
    }

    public synchronized UserProfile fallbackUser(int slotId) {
        ClientSlots.requireSlotId(slotId);
        UserProfile primary = this.minecraftUser();
        String name = primary.displayName() + "_" + (slotId + 1);
        long now = System.currentTimeMillis();
        return new UserProfile(
            UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),
            name,
            new UserIcon(DEFAULT_ICON),
            UserType.FALLBACK,
            new UserOptions(),
            now,
            now
        );
    }

    public synchronized void markUsed(UserProfile user) {
        if (user.type() != UserType.LOCAL) {
            return;
        }
        UserProfile updated = user.usedAt(System.currentTimeMillis());
        this.localUsers.put(updated.id(), updated);
        this.save();
    }

    public synchronized void bind(int slotId, UserProfile user) {
        ClientSlots.requireSlotId(slotId);
        this.activeUsers.put(slotId, user.id());
        this.markUsed(user);
    }

    public synchronized void unbind(int slotId) {
        if (slotId != 0) {
            this.activeUsers.remove(slotId);
        }
    }

    public synchronized @Nullable Integer activeSlot(UUID userId) {
        this.ensureLoaded();
        this.minecraftUser();
        for (Map.Entry<Integer, UUID> entry : this.activeUsers.entrySet()) {
            if (entry.getValue().equals(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean nameExists(String name) {
        if (this.minecraftUser().displayName().equalsIgnoreCase(name)) {
            return true;
        }
        return this.localUsers.values().stream()
            .anyMatch(user -> user.displayName().equalsIgnoreCase(name));
    }

    private void ensureLoaded() {
        if (this.loaded) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        this.file = minecraft.gameDirectory.toPath().resolve(FILE_NAME);
        JsonObject root = JsonFileStore.read(this.file);
        if (root != null && root.get("users") instanceof JsonArray users) {
            for (JsonElement element : users) {
                if (element instanceof JsonObject object) {
                    UserProfile profile = readUser(object);
                    if (profile != null && profile.type() == UserType.LOCAL) {
                        this.localUsers.put(profile.id(), profile);
                    }
                }
            }
        }
        this.loaded = true;
        this.minecraftUser();
    }

    private void save() {
        if (!this.loaded || this.file == null) {
            return;
        }
        JsonArray users = new JsonArray();
        for (UserProfile profile : this.localUsers.values()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", profile.id().toString());
            object.addProperty("display_name", profile.displayName());
            object.addProperty("icon", profile.icon().texture());
            object.addProperty("type", profile.type().name().toLowerCase(Locale.ROOT));
            object.add("options", new JsonObject());
            object.addProperty("created_at", profile.createdAt());
            object.addProperty("last_used_at", profile.lastUsedAt());
            users.add(object);
        }
        JsonObject root = new JsonObject();
        root.add("users", users);
        JsonFileStore.write(this.file, root);
    }

    private static @Nullable UserProfile readUser(JsonObject object) {
        try {
            UUID id = UUID.fromString(object.get("id").getAsString());
            String name = object.get("display_name").getAsString();
            String icon = object.has("icon")
                ? object.get("icon").getAsString()
                : DEFAULT_ICON;
            long createdAt = object.has("created_at")
                ? object.get("created_at").getAsLong()
                : System.currentTimeMillis();
            long lastUsedAt = object.has("last_used_at")
                ? object.get("last_used_at").getAsLong()
                : createdAt;
            return new UserProfile(
                id,
                name,
                new UserIcon(icon),
                UserType.LOCAL,
                new UserOptions(),
                createdAt,
                lastUsedAt
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
