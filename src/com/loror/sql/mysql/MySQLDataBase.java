package com.loror.sql.mysql;

import com.loror.sql.SQLClient;
import com.loror.sql.SQLDataBase;
import com.mysql.jdbc.Statement;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class MySQLDataBase implements SQLDataBase {

    public interface OnGetPst {
        void getPst(PreparedStatement pst) throws SQLException;
    }

    private Connection conn;
    private SQLClient.LogListener logListener;

    public MySQLDataBase(String driver, String url, String name, String password, SQLClient.LogListener logListener) throws SQLException, ClassNotFoundException {
        Class.forName(driver);// 指定连接类型
        conn = DriverManager.getConnection(url, name, password);// 获取连接
        this.logListener = logListener;
    }

    public Connection getConn() {
        return conn;
    }

    public PreparedStatement getPst(String sql, boolean returnKey) throws SQLException {
        if (conn == null) {
            return null;
        }
        if (logListener != null) {
            logListener.log(sql);
        }
        if (returnKey) {
            return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);// 准备执行语句
        } else {
            return conn.prepareStatement(sql);// 准备执行语句
        }
    }

    public void getPst(String sql, boolean returnKey, OnGetPst onGetPst) throws SQLException {
        PreparedStatement preparedStatement = getPst(sql, returnKey);
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

    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }
}
