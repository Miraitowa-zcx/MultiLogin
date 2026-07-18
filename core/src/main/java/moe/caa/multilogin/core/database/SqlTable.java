package moe.caa.multilogin.core.database;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlTable {
    void init(Connection connection) throws SQLException;
}
