package net.jr.client.compat.xaero;

import java.io.File;
import net.jr.client.runtime.context.LocalClientScope;

public final class XaeroWorldMapPaths {
    private XaeroWorldMapPaths() {
    }

    public static File currentRoot(File root) {
        if (LocalClientScope.currentClientOrNull() == null) {
            return root;
        }
        int slotId = LocalClientScope.currentClient().slotId();
        if (slotId == 0) {
            return root;
        }
        return root.toPath()
            .resolve("java_reforged")
            .resolve("slot_" + slotId)
            .toFile();
    }
}
