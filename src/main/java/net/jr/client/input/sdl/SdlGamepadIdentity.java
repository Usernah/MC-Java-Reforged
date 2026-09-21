package net.jr.client.input.sdl;

import dev.isxander.sdl3java.api.guid.SDL_GUID;
import dev.isxander.sdl3java.api.joystick.SDL_Joystick;
import dev.isxander.sdl3java.api.joystick.SDL_JoystickID;
import dev.isxander.sdl3java.api.joystick.SdlJoystick;
import java.util.Locale;
import net.jr.api.client.input.GamepadIdentity;

final class SdlGamepadIdentity {
    private SdlGamepadIdentity() {
    }

    static GamepadIdentity fromDeviceId(SDL_JoystickID deviceId) {
        return create(
            safe(SdlJoystick.SDL_GetJoystickNameForID(deviceId), "SDL Controller"),
            unsigned(SdlJoystick.SDL_GetJoystickVendorForID(deviceId)),
            unsigned(SdlJoystick.SDL_GetJoystickProductForID(deviceId)),
            unsigned(SdlJoystick.SDL_GetJoystickProductVersionForID(deviceId)),
            guid(SdlJoystick.SDL_GetJoystickGUIDForID(deviceId))
        );
    }

    static GamepadIdentity fromJoystick(SDL_Joystick joystick) {
        if (joystick == null) return fallback("SDL Controller");
        SDL_JoystickID joystickId = SdlJoystick.SDL_GetJoystickID(joystick);
        return create(
            safe(SdlJoystick.SDL_GetJoystickName(joystick), "SDL Controller"),
            unsigned(SdlJoystick.SDL_GetJoystickVendor(joystick)),
            unsigned(SdlJoystick.SDL_GetJoystickProduct(joystick)),
            unsigned(SdlJoystick.SDL_GetJoystickProductVersion(joystick)),
            joystickId == null ? "" : guid(SdlJoystick.SDL_GetJoystickGUIDForID(joystickId))
        );
    }

    static GamepadIdentity fallback(String name) {
        return create(name, 0, 0, 0, "");
    }

    static String guid(SDL_GUID guid) {
        if (guid == null || guid.data == null || guid.data.length == 0) return "";
        StringBuilder builder = new StringBuilder(guid.data.length * 2);
        for (byte value : guid.data) {
            builder.append(String.format(Locale.ROOT, "%02x", Byte.toUnsignedInt(value)));
        }
        return builder.toString();
    }

    static int unsigned(short value) {
        return Short.toUnsignedInt(value);
    }

    static int unsigned(char value) {
        return value & 0xFFFF;
    }

    private static GamepadIdentity create(String name, int vendor, int product, int version, String guid) {
        String safeName = safe(name, "SDL Controller");
        String sanitizedName = sanitize(safeName);
        String base = vendor != 0 || product != 0
            ? String.format(Locale.ROOT, "vid_%04x_pid_%04x_ver_%04x", vendor, product, version)
            : (!guid.isBlank() ? "guid_" + sanitize(guid) : "name_" + sanitizedName);
        return new GamepadIdentity(base + "_" + sanitizedName, safeName, vendor, product, version, guid);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }
}
