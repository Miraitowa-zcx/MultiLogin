package moe.caa.multilogin.core.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlIdentifiersTest {
    @Test
    void normalizesValidTablePrefix() {
        assertEquals("multilogin_", SqlIdentifiers.tablePrefix("multilogin"));
        assertEquals("tenant_01_", SqlIdentifiers.tablePrefix("tenant_01"));
    }

    @Test
    void rejectsUnsafeOrOversizedTablePrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlIdentifiers.tablePrefix("bad-prefix"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlIdentifiers.tablePrefix("x".repeat(41)));
        assertThrows(IllegalArgumentException.class,
                () -> SqlIdentifiers.tablePrefix(""));
    }
}
