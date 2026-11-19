package net.jr.screens.crafting;

import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.UiDataFile;
import net.jr.api.client.ui.UiDataValues;

final class CraftingScreenData {
    private final UiDataValues values;

    CraftingScreenData(UiDataFile file) {
        this.values = file.readData();
    }

    int integer(String id) {
        return this.values.integer(id);
    }

    float decimal(String id) {
        return this.values.decimal(id);
    }

    int color(String id) {
        return this.values.color(id);
    }

    String string(String id) {
        return this.values.string(id);
    }

    Asset asset(String id) {
        return this.values.asset(id);
    }
}
