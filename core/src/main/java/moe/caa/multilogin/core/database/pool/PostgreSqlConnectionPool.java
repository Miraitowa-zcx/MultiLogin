package moe.caa.multilogin.core.database.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import moe.caa.multilogin.core.database.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;

/** Hikari-backed PostgreSQL connection pool. */
public final class PostgreSqlConnectionPool implements ISQLConnectionPool {
    public static final String defaultUrl = "jdbc:postgresql://{0}:{1}/{2}";

    private final HikariDataSource dataSource;

    public PostgreSqlConnectionPool(
            String ip,
            int port,
            String database,
            String username,
            String password) throws ClassNotFoundException {
        this(ip, port, database, username, password, defaultUrl);
    }

    public PostgreSqlConnectionPool(
            String ip,
            int port,
            String database,
            String username,
            String password,
            String url) throws ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(expandUrl(url, ip, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(600_000);
        dataSource = new HikariDataSource(config);
    }

    public static String expandUrl(
            String url,
            String ip,
            int port,
            String database) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("PostgreSQL JDBC URL must not be blank");
        }
        return url.replace("{0}", ip)
                .replace("{1}", String.valueOf(port))
                .replace("{2}", database);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public String name() {
        return "PostgreSQL";
    }

    @Override
    public SqlDialect dialect() {
        return SqlDialect.POSTGRESQL;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
