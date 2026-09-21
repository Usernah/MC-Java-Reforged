package net.jr.util;

import java.util.List;

public final class LegacyPlayerColors {
    public static final List<Integer> COLORS = List.of(
            0xFFFFFF, 0x00ff20, 0xFF2119, 0x577bff,
            0xF436FA, 0xff6b00, 0x1A1A1A, 0x00f5ec,
            0x1017A3, 0xc9ff00, 0x7000B5, 0xffce00,
            0x773818, 0x006118, 0xFF99BB, 0x808080
    );

    private LegacyPlayerColors() {
    }

    public static int get(int index) {
        return COLORS.get(Math.floorMod(index, COLORS.size()));
    }

    public static int size() {
        return COLORS.size();
    }
}
