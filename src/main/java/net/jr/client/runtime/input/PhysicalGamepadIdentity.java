package net.jr.client.runtime.input;

/** Model plus a physical discriminator supplied by the device backend. */
record PhysicalGamepadIdentity(String modelKey, String serial, String path) {
    boolean sameController(PhysicalGamepadIdentity other) {
        return this.modelKey.equals(other.modelKey)
            && ((!this.serial.isBlank() && !other.serial.isBlank() && this.serial.equals(other.serial))
                || (!this.path.isBlank() && !other.path.isBlank() && this.path.equals(other.path)));
    }
}
