package moe.caa.multilogin.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Owns the transaction that creates and migrates all SQL tables. */
public final class SqlSchemaInitializer {
    private SqlSchemaInitializer() {
    }

    public static void initialize(SqlDatabase database, List<? extends SqlTable> tables)
            throws SQLException {
        try (Connection connection = database.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (SqlTable table : tables) {
                    table.init(connection);
                }
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
