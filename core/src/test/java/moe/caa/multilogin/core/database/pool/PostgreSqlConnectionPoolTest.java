package moe.caa.multilogin.core.database.pool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgreSqlConnectionPoolTest {
    @Test
    void expandsDefaultConnectionUrl() {
        assertEquals(
                "jdbc:postgresql://db.example:5432/multilogin",
                PostgreSqlConnectionPool.expandUrl(
                        PostgreSqlConnectionPool.defaultUrl,
                        "db.example",
                        5432,
                        "multilogin"));
    }
}
