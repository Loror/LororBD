package com.loror.sql;

public interface SQLDataBase {

    ModelDataList executeQuery(String sql);

    boolean execute(String sql);

    ModelData executeByReturnKeys(String sql);

    int executeUpdate(String sql);

    void close() throws Exception;

    boolean isClosed();
}
