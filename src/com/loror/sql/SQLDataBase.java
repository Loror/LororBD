package com.loror.sql;

public interface SQLDataBase {

    ModelResultList executeQuery(String sql);

    boolean execute(String sql);

    ModelResult executeByReturnKeys(String sql);

    int executeUpdate(String sql);

    void close() throws Exception;
}
