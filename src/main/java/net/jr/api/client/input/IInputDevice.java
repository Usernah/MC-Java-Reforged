package net.jr.api.client.input;

/** A physical input source without backend-specific handles. */
public interface IInputDevice {
    String displayName();

    boolean connected();
}
