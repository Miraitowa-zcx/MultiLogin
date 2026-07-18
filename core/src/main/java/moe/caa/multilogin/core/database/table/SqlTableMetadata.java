package moe.caa.multilogin.core.database.table;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

final class SqlTableMetadata {
    private SqlTableMetadata() {
    }

    static boolean tableExists(Connection connection, String tableName)
            throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String lookupName = normalizedIdentifier(metadata, tableName);
        String pattern = escapedPattern(metadata, lookupName);
        try (ResultSet tables = metadata.getTables(
                connection.getCatalog(),
                connection.getSchema(),
                pattern,
                new String[]{"TABLE"})) {
            while (tables.next()) {
                if (lookupName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalizedIdentifier(
            DatabaseMetaData metadata,
            String identifier) throws SQLException {
        if (metadata.storesLowerCaseIdentifiers()) {
            return identifier.toLowerCase(Locale.ROOT);
        }
        if (metadata.storesUpperCaseIdentifiers()) {
            return identifier.toUpperCase(Locale.ROOT);
        }
        return identifier;
    }

    private static String escapedPattern(
            DatabaseMetaData metadata,
            String identifier) throws SQLException {
        String escape = metadata.getSearchStringEscape();
        if (escape == null || escape.isEmpty()) {
            return identifier;
        }
        return identifier.replace(escape, escape + escape)
                .replace("_", escape + "_")
                .replace("%", escape + "%");
    }
}
