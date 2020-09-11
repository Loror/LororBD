package com.loror.sql;

import com.loror.sql.mysql.MySQLClient;

public interface SQLClient {

    interface OnClose {
        void close(MySQLClient mySqlClient);
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
     * 获取条件处理model，由注解确定是否检测table
     */
    <T> Model<T> model(Class<T> table);

    /**
     * 获取条件处理model，用户确定是否检测table
     */
    <T> Model<T> model(Class<T> table, boolean checkTable);

    /**
     * 事务
     */
    boolean transaction(Runnable runnable);

}
