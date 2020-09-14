package com.loror.sql;

public interface NativeQuery {

    ModelResultList select(String sql);

    void execute(String sql);

    int executeUpdate(String sql);
}
