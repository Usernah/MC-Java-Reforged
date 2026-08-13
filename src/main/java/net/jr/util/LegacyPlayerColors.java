package net.jr.util;

import java.util.List;

public final class LegacyPlayerColors {
    public static final List<Integer> COLORS = List.of(
            0xFFFFFF, 0x00FF4C, 0xFF2119, 0x6385FF,
            0xFF63D9, 0xFF9C00, 0xFFFB19, 0x63FFE4,
            0x1D64E4, 0xD5FF9D, 0x8F2DE2, 0xFDD703,
            0xBD2B09, 0x348300, 0xF5B3F6, 0x8E4C22
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
