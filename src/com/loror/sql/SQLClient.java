package com.loror.sql;

import java.io.Closeable;

public interface SQLClient extends Closeable {

    interface OnClose {
        void close(SQLClient sqlClient);
    }

    interface LogListener {
        void log(boolean connect, String sql);
    }

    interface SQLCache {
        ModelResultList beforeQuery(String sql);

        void onExecuteQuery(String sql, ModelResultList modelResults);

        void onExecute(String sql);
    }

    /**
     * 重新开启
     */
    void reStart();

    /**
     * 关闭
     */
    void close();

    /**
     * 是否关闭
     */
    boolean isClosed();

    /**
     * 代理close方法
     */
    void setOnClose(OnClose onClose);

    /**
     * 代理log
     */
    void setLogListener(LogListener logListener);

    /**
     * 设置sql缓存
     */
    void setSQLCache(SQLCache sqlCache);

    /**
     * 创建表
     */
    void createTableIfNotExists(Class<?> table);

    /**
     * 删除表
     */
    void dropTable(Class<?> table);

    /**
     * 表字段增加
     * 注意：执行更新表后，可能表未及时变化，下次执行会再次执行更新表而报错，需保证close之前只执行一次
     */
    void changeTableIfColumnAdd(Class<?> table);

    /**
     * 获取model信息
     */
    ModelInfo getModel(Class<?> table);

    /**
     * 获取条件处理model
     */
    Model model(String table);

    /**
     * 事务
     */
    boolean transaction(Runnable runnable);

    /**
     * 获取原生执行
     */
    SQLDataBase nativeQuery();

}
