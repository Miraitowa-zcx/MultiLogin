package moe.caa.multilogin.core.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Connection and dialect boundary consumed by SQL tables. */
public interface SqlDatabase {
    Connection getConnection() throws SQLException;

    SqlDialect dialect();
}
