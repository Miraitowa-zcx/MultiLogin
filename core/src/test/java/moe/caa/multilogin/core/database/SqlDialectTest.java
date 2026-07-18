package moe.caa.multilogin.core.database;

import org.junit.jupiter.api.Test;

import static moe.caa.multilogin.core.database.SqlDialect.MYSQL;
import static moe.caa.multilogin.core.database.SqlDialect.POSTGRESQL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlDialectTest {
    @Test
    void mapsBinaryAndLargeTextTypes() {
        assertEquals("BYTEA", POSTGRESQL.binaryType(16));
        assertEquals("TEXT", POSTGRESQL.largeTextType());
        assertEquals("BINARY(32)", MYSQL.binaryType(32));
    }

    @Test
    void constrainsPostgresqlBinaryLength() {
        assertEquals(
                "CHECK (octet_length(online_uuid) = 16)",
                POSTGRESQL.binaryCheck("online_uuid", 16));
        assertEquals("", MYSQL.binaryCheck("online_uuid", 16));
    }
}
