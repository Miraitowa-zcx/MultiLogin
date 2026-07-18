package moe.caa.multilogin.core.database;

/** DDL differences supported by MultiLogin's SQL backends. */
public enum SqlDialect {
    H2,
    MYSQL,
    POSTGRESQL;

    public String binaryType(int bytes) {
        requirePositiveLength(bytes);
        return this == POSTGRESQL ? "BYTEA" : "BINARY(" + bytes + ")";
    }

    public String binaryCheck(String column, int bytes) {
        requirePositiveLength(bytes);
        if (this != POSTGRESQL) {
            return "";
        }
        if (column == null || !column.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe SQL column identifier: " + column);
        }
        return "CHECK (octet_length(" + column + ") = " + bytes + ")";
    }

    public String largeTextType() {
        return this == POSTGRESQL ? "TEXT" : "LONGTEXT";
    }

    private static void requirePositiveLength(int bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("Binary length must be positive");
        }
    }
}
