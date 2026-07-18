package moe.caa.multilogin.core.database.table;

import moe.caa.multilogin.api.internal.util.Pair;
import moe.caa.multilogin.core.database.SqlDatabase;
import moe.caa.multilogin.core.database.SqlDialect;
import moe.caa.multilogin.core.database.SqlTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 皮肤修复缓存表
 */
public class SkinRestoredCacheTableV2 implements SqlTable {
    private static final String fieldCurrentSkinUrlSha256 = "current_skin_url_sha256";
    private static final String fieldCurrentSkinModel = "current_skin_model";
    private static final String fieldRestorerValue = "restorer_value";
    private static final String fieldRestorerSignature = "restorer_signature";
    private final SqlDatabase database;
    private final String tableName;

    public SkinRestoredCacheTableV2(SqlDatabase database, String tableName) {
        this.database = database;
        this.tableName = tableName;
    }

    @Override
    public void init(Connection connection) throws SQLException {
        SqlDialect dialect = database.dialect();
        String digestCheck = dialect.binaryCheck(fieldCurrentSkinUrlSha256, 32);
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s ( "
                        + "%s %s NOT NULL%s, "
                        + "%s VARCHAR(16) NOT NULL, "
                        + "%s %s NOT NULL, "
                        + "%s %s NOT NULL, "
                        + "PRIMARY KEY ( %s, %s ))",
                tableName,
                fieldCurrentSkinUrlSha256,
                dialect.binaryType(32),
                digestCheck.isEmpty() ? "" : " " + digestCheck,
                fieldCurrentSkinModel,
                fieldRestorerValue,
                dialect.largeTextType(),
                fieldRestorerSignature,
                dialect.largeTextType(),
                fieldCurrentSkinUrlSha256,
                fieldCurrentSkinModel);
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }
    }

    /**
     * 获得缓存的数据对象
     *
     * @param urlSha256 皮肤 URL
     * @param model     皮肤模型
     * @return 缓存的对象
     */
    public Pair<String, String> getCacheRestored(byte[] urlSha256, String model) throws SQLException {
        String sql = String.format(
                "SELECT %s, %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , fieldRestorerValue, fieldRestorerSignature, tableName, fieldCurrentSkinUrlSha256, fieldCurrentSkinModel
        );
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, urlSha256);
            statement.setString(2, model);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Pair<>(resultSet.getString(1), resultSet.getString(2));
                }
            }
        }
        return null;
    }

    /**
     * 插入新的缓存对象
     *
     * @param urlSha256 皮肤 URL
     * @param model     皮肤模型
     * @param value     值
     * @param signature 签名
     */
    public void insertNew(byte[] urlSha256, String model, String value, String signature) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?) "
                , tableName, fieldCurrentSkinUrlSha256, fieldCurrentSkinModel, fieldRestorerValue, fieldRestorerSignature
        );
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, urlSha256);
            statement.setString(2, model);
            statement.setString(3, value);
            statement.setString(4, signature);
            statement.executeUpdate();
        }
    }
}
