package net.jr.client.ui.container.actions;

public interface CreativeCatalogActionHost {

    boolean handleCreativeQuickMove();

    Target creativeCatalogTarget();

    enum Target {
        NONE,
        CATALOG_ITEM,
        HOTBAR_ITEM
    }
}
