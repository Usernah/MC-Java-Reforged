package net.jr.client.runtime.input;

import javax.annotation.Nullable;


interface ReconnectableGamepad {
    @Nullable PhysicalGamepadIdentity physicalIdentity();
}
