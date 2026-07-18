package moe.caa.multilogin.velocity.injector.compat;

/**
 * Indicates that the running Velocity build no longer exposes an internal
 * contract required by MultiLogin's authentication interception.
 */
public final class VelocityCompatibilityException extends RuntimeException {
    public VelocityCompatibilityException(String message) {
        super(message);
    }

    public VelocityCompatibilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
