package net.jr.client.compat.xaero;

import java.io.File;

import net.jr.api.client.local.Client;

public final class XaeroWorldMapPaths {
    private XaeroWorldMapPaths() {
    }

    public static File currentRoot(File root) {
        if (Client.currentOrNull() == null) {
            return root;
        }
        int slotId = Client.slotId();
        if (slotId == 0) {
            return root;
        }
        return root.toPath()
            .resolve("java_reforged")
            .resolve("slot_" + slotId)
            .toFile();
    }
}
