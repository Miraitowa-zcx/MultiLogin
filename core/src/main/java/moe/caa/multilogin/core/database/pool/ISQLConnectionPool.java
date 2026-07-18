package moe.caa.multilogin.core.database.pool;

import moe.caa.multilogin.core.database.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 表示数据库连接池
 */
public interface ISQLConnectionPool extends AutoCloseable {
    /**
     * 获得链接对象
     *
     * @return 链接对象
     */
    Connection getConnection() throws SQLException;

    /**
     * 获得该连接池名字
     */
    String name();

    /** SQL dialect exposed to schema initialization. */
    SqlDialect dialect();

    /**
     * 关闭链接
     */
    @Override
    void close();
}
