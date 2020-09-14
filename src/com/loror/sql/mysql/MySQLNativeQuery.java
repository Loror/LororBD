package com.loror.sql.mysql;

import com.loror.sql.ModelResult;
import com.loror.sql.ModelResultList;
import com.loror.sql.NativeQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MySQLNativeQuery implements NativeQuery {

    private MySQLDataBase mySQLDataBase;

    public MySQLNativeQuery(MySQLDataBase mySQLDataBase) {
        this.mySQLDataBase = mySQLDataBase;
    }

    @Override
    public ModelResultList select(String sql) {
        ModelResultList entitys = new ModelResultList();
        try {
            mySQLDataBase.getPst(sql, false, pst -> {
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
            mySQLDataBase.getPst(sql, false, pst -> {
                execute[0] = pst.execute();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return execute[0];
    }

    @Override
    public int executeUpdate(String sql) {
        int[] updates = new int[1];
        try {
            mySQLDataBase.getPst(sql, false, pst -> {
                updates[0] = pst.executeUpdate();
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updates[0];
    }

}
