package moe.caa.multilogin.core.database;

import java.util.regex.Pattern;

/** Validates configured identifiers before they can be interpolated into SQL. */
public final class SqlIdentifiers {
    private static final int MAX_CONFIGURED_PREFIX_LENGTH = 40;
    private static final Pattern SAFE_PREFIX = Pattern.compile("[A-Za-z0-9_]+");

    private SqlIdentifiers() {
    }

    public static String tablePrefix(String configuredPrefix) {
        if (configuredPrefix == null || configuredPrefix.isEmpty()) {
            throw new IllegalArgumentException("SQL table prefix must not be empty");
        }
        if (configuredPrefix.length() > MAX_CONFIGURED_PREFIX_LENGTH) {
            throw new IllegalArgumentException(
                    "SQL table prefix exceeds " + MAX_CONFIGURED_PREFIX_LENGTH + " characters");
        }
        if (!SAFE_PREFIX.matcher(configuredPrefix).matches()) {
            throw new IllegalArgumentException(
                    "SQL table prefix may contain only ASCII letters, digits, and underscores");
        }
        return configuredPrefix + '_';
    }
}
