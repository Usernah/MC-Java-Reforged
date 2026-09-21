package net.jr.client.runtime.input;

import javax.annotation.Nullable;

/** Backend capability used only while recovering a retained assignment. */
interface ReconnectableGamepad {
    @Nullable PhysicalGamepadIdentity physicalIdentity();
}
