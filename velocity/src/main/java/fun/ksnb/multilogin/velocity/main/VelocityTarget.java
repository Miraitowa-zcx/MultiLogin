package fun.ksnb.multilogin.velocity.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Compile-time identity of the verified official Velocity artifact. */
public record VelocityTarget(
        String version,
        int build,
        String commit,
        String sha256) {
    private static final String RESOURCE = "/velocity-target.properties";

    public VelocityTarget {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Velocity target version must not be blank");
        }
        if (build <= 0) {
            throw new IllegalArgumentException("Velocity target build must be positive");
        }
        if (commit == null || !commit.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Velocity target commit must be a full SHA-1");
        }
        if (sha256 == null || !sha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Velocity target checksum must be SHA-256");
        }
    }

    public static VelocityTarget load() {
        try (InputStream input = VelocityTarget.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Missing generated Velocity target resource: " + RESOURCE);
            }
            Properties properties = new Properties();
            properties.load(input);
            return from(properties);
        } catch (IOException | IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "Invalid generated Velocity target resource: " + RESOURCE,
                    failure);
        }
    }

    static VelocityTarget from(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        String version = required(properties, "velocity.version");
        String build = required(properties, "velocity.build");
        String commit = required(properties, "velocity.commit");
        String sha256 = required(properties, "velocity.sha256");
        try {
            return new VelocityTarget(
                    version,
                    Integer.parseInt(build),
                    commit.toLowerCase(),
                    sha256.toLowerCase());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "Velocity target build is not numeric: " + build,
                    failure);
        }
    }

    public String displayName() {
        return version + " build " + build + " (git-" + commit.substring(0, 8) + ")";
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing Velocity target property: " + key);
        }
        return value.trim();
    }
}
