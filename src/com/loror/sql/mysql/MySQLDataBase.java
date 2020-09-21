package com.loror.sql.mysql;

import com.loror.sql.ModelResult;
import com.loror.sql.ModelResultList;
import com.loror.sql.SQLDataBase;
import com.mysql.jdbc.Statement;

import java.sql.*;
import java.util.List;

abstract class MySQLDataBase implements SQLDataBase {

    public interface OnGetPst {
        void getPst(PreparedStatement pst) throws SQLException;
    }

    private Connection conn;

    public MySQLDataBase(String driver, String url, String name, String password) throws SQLException, ClassNotFoundException {
        Class.forName(driver);// 指定连接类型
        conn = DriverManager.getConnection(url, name, password);// 获取连接
    }

    public Connection getConn() {
        return conn;
    }

    public PreparedStatement getPst(String sql, boolean returnKey) throws SQLException {
        if (conn == null || conn.isClosed()) {
            return null;
        }
        onSql(true, sql);
        if (returnKey) {
            return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);// 准备执行语句
        } else {
            return conn.prepareStatement(sql);// 准备执行语句
        }
    }

    public void getPst(String sql, boolean returnKey, OnGetPst onGetPst) throws SQLException {
        PreparedStatement preparedStatement = getPst(sql, returnKey);
        if (preparedStatement == null) {
            return;
        }
        try {
            if (onGetPst != null) {
                onGetPst.getPst(preparedStatement);
            } else {
                preparedStatement.execute();
            }
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw e;
            }
            e.printStackTrace();
        } finally {
            preparedStatement.close();
        }
    }

    public abstract void onSql(boolean connect, String sql);

    public abstract ModelResultList beforeQuery(String sql);

    @Override
    public ModelResultList executeQuery(String sql) {
        ModelResultList cache = beforeQuery(sql);
        if (cache != null) {
            onSql(false, sql);
            return cache;
        }
        ModelResultList entitys = new ModelResultList();
        try {
            getPst(sql, false, pst -> {
                ResultSet cursor = pst.executeQuery();
                List<ModelResult> modelResults = MySQLResult.find(cursor);
                cursor.close();
                entitys.addAll(modelResults);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entitys;
    }

    @Override
    public boolean execute(String sql) {
        boolean[] execute = new boolean[1];
        try {
            getPst(sql, false, pst -> {
                execute[0] = pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return execute[0];
    }

    @Override
    public ModelResult executeByReturnKeys(String sql) {
        ModelResult result = new ModelResult();
        try {
            getPst(sql, true, pst -> {
                pst.execute();
                // 在执行更新后获取自增长列
                ResultSet cursor = pst.getGeneratedKeys();
                List<ModelResult> modelResults = MySQLResult.find(cursor);
                cursor.close();
                if (modelResults.size() > 0) {
                    ModelResult query = modelResults.get(0);
                    result.addAll(query);
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int executeUpdate(String sql) {
        int[] updates = new int[1];
        try {
            getPst(sql, false, pst -> {
                updates[0] = pst.executeUpdate();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updates[0];
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
            conn = null;
        }
    }

    public boolean isClosed() {
        try {
            return conn == null || conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}
