package moe.caa.multilogin.core.database.table;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import moe.caa.multilogin.api.internal.util.ValueUtil;
import moe.caa.multilogin.core.database.SqlDatabase;
import moe.caa.multilogin.core.database.SqlDialect;
import moe.caa.multilogin.core.database.SqlSchemaInitializer;
import moe.caa.multilogin.core.database.pool.PostgreSqlConnectionPool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgreSqlV2MigrationTest {
    @Test
    void migratesV2ProfileAndUserWithoutChangingStoredIdentity() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
             PostgreSqlConnectionPool pool = new PostgreSqlConnectionPool(
                     "127.0.0.1", postgres.getPort(), "postgres", "postgres", "",
                     postgres.getJdbcUrl("postgres", "postgres"))) {
            SqlDatabase database = database(pool);
            byte[] inGameUuid = ValueUtil.uuidToBytes(UUID.randomUUID());
            byte[] onlineUuid = ValueUtil.uuidToBytes(UUID.randomUUID());
            createV2Tables(database, inGameUuid, onlineUuid);

            InGameProfileTableV3 profiles = new InGameProfileTableV3(
                    database, "migration_in_game_profile_v3", "migration_in_game_profile_v2");
            UserDataTableV3 users = new UserDataTableV3(
                    database, "migration_user_data_v3", "migration_user_data_v2");
            SqlSchemaInitializer.initialize(database, List.of(profiles, users));

            try (Connection connection = database.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT in_game_uuid, current_username_lower_case, "
                                + "current_username_original "
                                + "FROM migration_in_game_profile_v3");
                     ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertArrayEquals(inGameUuid, resultSet.getBytes(1));
                    assertEquals("mixedcase", resultSet.getString(2));
                    assertNull(resultSet.getString(3));
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT online_uuid, service_id, in_game_profile_uuid, whitelist "
                                + "FROM migration_user_data_v3");
                     ResultSet resultSet = statement.executeQuery()) {
                    assertTrue(resultSet.next());
                    assertArrayEquals(onlineUuid, resultSet.getBytes(1));
                    assertEquals(7, resultSet.getInt(2));
                    assertArrayEquals(inGameUuid, resultSet.getBytes(3));
                    assertTrue(resultSet.getBoolean(4));
                }
            }
        }
    }

    private static void createV2Tables(
            SqlDatabase database,
            byte[] inGameUuid,
            byte[] onlineUuid) throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE migration_in_game_profile_v2 "
                    + "(in_game_uuid BYTEA NOT NULL, current_username VARCHAR(64))");
            statement.executeUpdate("CREATE TABLE migration_user_data_v2 "
                    + "(online_uuid BYTEA NOT NULL, yggdrasil_id BYTEA NOT NULL, "
                    + "in_game_profile_uuid BYTEA, whitelist BOOLEAN NOT NULL)");
        }
        try (Connection connection = database.getConnection();
             PreparedStatement profile = connection.prepareStatement(
                     "INSERT INTO migration_in_game_profile_v2 VALUES (?, ?)");
             PreparedStatement user = connection.prepareStatement(
                     "INSERT INTO migration_user_data_v2 VALUES (?, ?, ?, ?)")) {
            profile.setBytes(1, inGameUuid);
            profile.setString(2, "MiXeDCase");
            profile.executeUpdate();
            user.setBytes(1, onlineUuid);
            user.setBytes(2, new byte[]{7});
            user.setBytes(3, inGameUuid);
            user.setBoolean(4, true);
            user.executeUpdate();
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
