package net.jr.client.compat.sodium;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * State boundary installed on Sodium's shared section manager by the runtime
 * class processor. The cache remains typed as Object so this contract can be
 * loaded when Sodium is absent.
 */
public interface SodiumTerrainSourceAccess {
    void javaReforged$setTerrainSource(ClientLevel level, Object sectionCache);
}
