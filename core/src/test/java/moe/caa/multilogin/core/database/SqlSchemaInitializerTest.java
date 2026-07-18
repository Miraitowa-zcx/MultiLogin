package moe.caa.multilogin.core.database;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import moe.caa.multilogin.core.database.pool.PostgreSqlConnectionPool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSchemaInitializerTest {
    @Test
    void rollsBackEveryTableWhenInitializationFails() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
             PostgreSqlConnectionPool pool = new PostgreSqlConnectionPool(
                     "127.0.0.1", postgres.getPort(), "postgres", "postgres", "",
                     postgres.getJdbcUrl("postgres", "postgres"))) {
            SqlDatabase database = database(pool);
            SqlTable createMarker = connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE rollback_marker(id INTEGER)");
                }
            };
            SqlTable fail = connection -> {
                throw new SQLException("expected failure");
            };

            assertThrows(SQLException.class, () ->
                    SqlSchemaInitializer.initialize(database, List.of(createMarker, fail)));

            try (Connection connection = database.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                                 + "WHERE table_schema = 'public' "
                                 + "AND table_name = 'rollback_marker')")) {
                resultSet.next();
                assertFalse(resultSet.getBoolean(1));
            }
        }
    }

    private static SqlDatabase database(PostgreSqlConnectionPool pool) {
        return new SqlDatabase() {
            @Override
            public Connection getConnection() throws SQLException {
                return pool.getConnection();
            }

            @Override
            public SqlDialect dialect() {
                return pool.dialect();
            }
        };
    }
}
