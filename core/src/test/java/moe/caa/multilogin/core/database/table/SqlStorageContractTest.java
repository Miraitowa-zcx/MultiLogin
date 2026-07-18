package moe.caa.multilogin.core.database.table;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import moe.caa.multilogin.api.internal.util.Pair;
import moe.caa.multilogin.api.internal.util.There;
import moe.caa.multilogin.core.database.SqlDatabase;
import moe.caa.multilogin.core.database.SqlDialect;
import moe.caa.multilogin.core.database.SqlSchemaInitializer;
import moe.caa.multilogin.core.database.pool.H2ConnectionPool;
import moe.caa.multilogin.core.database.pool.ISQLConnectionPool;
import moe.caa.multilogin.core.database.pool.PostgreSqlConnectionPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqlStorageContractTest {
    @TempDir
    Path temporaryDirectory;

    private EmbeddedPostgres postgres;
    private PostgreSqlConnectionPool postgresqlPool;

    @BeforeAll
    void startPostgresql() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
        postgresqlPool = new PostgreSqlConnectionPool(
                "127.0.0.1",
                postgres.getPort(),
                "postgres",
                "postgres",
                "",
                postgres.getJdbcUrl("postgres", "postgres"));
    }

    @AfterAll
    void stopPostgresql() throws Exception {
        if (postgresqlPool != null) {
            postgresqlPool.close();
        }
        if (postgres != null) {
            postgres.close();
        }
    }

    @Test
    void createsPostgresqlSchema() throws Exception {
        SqlDatabase database = new TestDatabase(postgresqlPool);
        TableSet tables = tables(database, "contract_schema_");
        initialize(database, tables);

        assertEquals("bytea", columnType("contract_schema_in_game_profile_v3", "in_game_uuid"));
        assertEquals("bytea", columnType("contract_schema_user_data_v3", "online_uuid"));
        assertEquals("bytea", columnType(
                "contract_schema_skin_restored_cache_v2",
                "current_skin_url_sha256"));
        assertEquals("text", columnType(
                "contract_schema_skin_restored_cache_v2",
                "restorer_value"));
        assertEquals("text", columnType(
                "contract_schema_skin_restored_cache_v2",
                "restorer_signature"));
    }

    @Test
    void initializesFreshPostgresqlSchemaTransactionallyWithoutV2Tables() throws Exception {
        SqlDatabase database = new TestDatabase(postgresqlPool);
        TableSet tables = tables(database, "fresh_transaction_pg_");

        SqlSchemaInitializer.initialize(database, List.of(
                tables.users(),
                tables.profiles(),
                tables.skins()));

        assertEquals("bytea", columnType(
                "fresh_transaction_pg_user_data_v3", "online_uuid"));
        assertEquals("bytea", columnType(
                "fresh_transaction_pg_in_game_profile_v3", "in_game_uuid"));
        assertEquals("bytea", columnType(
                "fresh_transaction_pg_skin_restored_cache_v2",
                "current_skin_url_sha256"));
    }

    @Test
    void supportsCrudContractOnPostgresql() throws Exception {
        SqlDatabase database = new TestDatabase(postgresqlPool);
        runCrudContract(database, "contract_pg_");
        assertPostgresqlLengthChecks(database, "contract_pg_");
    }

    @Test
    void supportsCrudContractOnH2() throws Exception {
        H2ConnectionPool pool = new H2ConnectionPool(
                temporaryDirectory.resolve("h2-contract").toFile(),
                "sa",
                "");
        try {
            runCrudContract(new TestDatabase(pool), "contract_h2_");
        } finally {
            pool.close();
        }
    }

    private void runCrudContract(SqlDatabase database, String prefix) throws Exception {
        TableSet tables = tables(database, prefix);
        initialize(database, tables);

        UUID inGameUuid = UUID.randomUUID();
        tables.profiles().insertNewData(inGameUuid, "PlayerOne");
        assertTrue(tables.profiles().dataExists(inGameUuid));
        assertEquals(inGameUuid, tables.profiles().getInGameUUIDIgnoreCase("pLaYeRoNe"));
        assertEquals("PlayerOne", tables.profiles().getUsername(inGameUuid));
        Pair<UUID, String> profile = tables.profiles().get(inGameUuid);
        assertEquals(inGameUuid, profile.getValue1());
        assertEquals("PlayerOne", profile.getValue2());

        assertThrows(SQLException.class, () -> tables.profiles().insertNewData(
                UUID.randomUUID(),
                "PLAYERONE"));
        tables.profiles().updateUsername(inGameUuid, "RenamedPlayer");
        assertEquals(inGameUuid, tables.profiles().getInGameUUIDIgnoreCase("renamedplayer"));

        UUID onlineOne = UUID.randomUUID();
        UUID onlineTwo = UUID.randomUUID();
        assertEquals(1, tables.users().insertNewData(
                onlineOne,
                1,
                "OnlineOne",
                inGameUuid));
        assertEquals(1, tables.users().insertNewData(
                onlineTwo,
                2,
                "OnlineTwo",
                inGameUuid));
        assertEquals(1, tables.users().insertNewData(
                UUID.randomUUID(),
                3,
                "Unlinked",
                null));

        There<String, UUID, Boolean> user = tables.users().get(onlineOne, 1);
        assertEquals("OnlineOne", user.getValue1());
        assertEquals(inGameUuid, user.getValue2());
        assertFalse(user.getValue3());
        assertEquals(onlineOne, tables.users().getOnlineUUID("onlineone", 1));
        assertEquals(inGameUuid, tables.users().getInGameUUID(onlineOne, 1));
        assertEquals(2, tables.users().getOnlineProfiles(inGameUuid).size());
        assertEquals(2, tables.users().getOnlineServiceIds(inGameUuid).size());

        tables.users().setOnlineName(onlineOne, 1, "OnlineOneRenamed");
        assertEquals("OnlineOneRenamed", tables.users().getOnlineName(onlineOne, 1));
        tables.users().setWhitelist(onlineOne, 1, true);
        assertTrue(tables.users().hasWhitelist(onlineOne, 1));
        assertTrue(tables.users().hasWhitelist(inGameUuid));
        tables.users().setWhitelist(inGameUuid, false);
        assertFalse(tables.users().hasWhitelist(onlineOne, 1));
        assertFalse(tables.users().hasWhitelist(onlineTwo, 2));
        assertFalse(tables.users().hasWhitelist(inGameUuid));

        tables.users().setWhitelist(onlineTwo, 2, true);
        List<String> whitelist = tables.users().listWhitelist(false);
        assertEquals(List.of("OnlineTwo"), whitelist);

        UUID replacementInGameUuid = UUID.randomUUID();
        assertEquals(1, tables.users().setInGameUUID(onlineOne, 1, replacementInGameUuid));
        assertEquals(replacementInGameUuid, tables.users().getInGameUUID(onlineOne, 1));

        byte[] digest = new byte[32];
        Arrays.fill(digest, (byte) 0x5a);
        String value = "value-" + "v".repeat(20_000);
        String signature = "signature-" + "s".repeat(20_000);
        tables.skins().insertNew(digest, "slim", value, signature);
        Pair<String, String> cached = tables.skins().getCacheRestored(digest, "slim");
        assertNotNull(cached);
        assertEquals(value, cached.getValue1());
        assertEquals(signature, cached.getValue2());

        assertTrue(tables.profiles().remove(inGameUuid));
        assertFalse(tables.profiles().dataExists(inGameUuid));
        assertNull(tables.profiles().get(inGameUuid));
    }

    private void assertPostgresqlLengthChecks(SqlDatabase database, String prefix) throws Exception {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + prefix + "in_game_profile_v3 "
                             + "(in_game_uuid, current_username_lower_case, current_username_original) "
                             + "VALUES (?, ?, ?)")) {
            statement.setBytes(1, new byte[15]);
            statement.setString(2, "invalid-length");
            statement.setString(3, "InvalidLength");
            assertThrows(SQLException.class, statement::executeUpdate);
        }

        TableSet tables = tables(database, prefix);
        assertThrows(SQLException.class, () -> tables.skins().insertNew(
                new byte[31],
                "slim",
                "value",
                "signature"));
    }

    private String columnType(String table, String column) throws Exception {
        try (Connection connection = postgresqlPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT data_type FROM information_schema.columns "
                             + "WHERE table_schema = 'public' AND table_name = ? "
                             + "AND column_name = ?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }
        }
    }

    private static TableSet tables(SqlDatabase database, String prefix) {
        return new TableSet(
                new InGameProfileTableV3(
                        database,
                        prefix + "in_game_profile_v3",
                        prefix + "in_game_profile_v2"),
                new UserDataTableV3(
                        database,
                        prefix + "user_data_v3",
                        prefix + "user_data_v2"),
                new SkinRestoredCacheTableV2(
                        database,
                        prefix + "skin_restored_cache_v2"));
    }

    private static void initialize(SqlDatabase database, TableSet tables) throws Exception {
        try (Connection connection = database.getConnection()) {
            tables.profiles().init(connection);
            tables.users().init(connection);
            tables.skins().init(connection);
        }
    }

    private record TestDatabase(ISQLConnectionPool pool) implements SqlDatabase {
        @Override
        public Connection getConnection() throws SQLException {
            return pool.getConnection();
        }

        @Override
        public SqlDialect dialect() {
            return pool.dialect();
        }
    }

    private record TableSet(
            InGameProfileTableV3 profiles,
            UserDataTableV3 users,
            SkinRestoredCacheTableV2 skins) {
    }
}
