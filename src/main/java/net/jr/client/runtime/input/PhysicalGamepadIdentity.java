package net.jr.client.runtime.input;


record PhysicalGamepadIdentity(String modelKey, String serial, String path) {
    boolean sameController(PhysicalGamepadIdentity other) {
        return this.modelKey.equals(other.modelKey)
            && ((!this.serial.isBlank() && !other.serial.isBlank() && this.serial.equals(other.serial))
                || (!this.path.isBlank() && !other.path.isBlank() && this.path.equals(other.path)));
    }
}
