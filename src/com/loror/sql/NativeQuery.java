package com.loror.sql;

public interface NativeQuery {

    ModelResultList select(String sql);

    boolean execute(String sql);

    int executeUpdate(String sql);
}
